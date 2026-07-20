package com.vita.workflow.runtime.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.workflow.api.WorkflowStartContextProvider;
import com.vita.workflow.api.command.WorkflowCancelCommand;
import com.vita.workflow.api.command.WorkflowRejectCommand;
import com.vita.workflow.api.command.WorkflowStartCommand;
import com.vita.workflow.api.command.WorkflowTaskCommand;
import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.api.event.WorkflowOperation;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowStartContext;
import com.vita.workflow.api.model.WorkflowStartResult;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.mapper.WorkflowRuntimeQueryMapper;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.support.WorkflowAfterCommitEventPublisher;
import com.vita.workflow.runtime.support.WorkflowDistributedLock;
import com.vita.workflow.runtime.support.WorkflowEngineExceptionTranslator;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import com.vita.workflow.runtime.vo.WorkflowTaskVo;
import com.vita.workflow.support.WorkflowLoginUserSupport;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.enums.ActivityStatus;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 基于 Warm-Flow 的通用工作流运行时实现
 * @Version: 1.0
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowRuntimeServiceImpl implements WorkflowRuntimeService {

    private final DefService defService;
    private final InsService insService;
    private final TaskService taskService;
    private final WorkflowRuntimeQueryMapper queryMapper;
    private final WorkflowLoginUserSupport loginUserSupport;
    private final List<WorkflowStartContextProvider> startContextProviders;
    private final WorkflowDistributedLock distributedLock;
    private final WorkflowAfterCommitEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public WorkflowRuntimeServiceImpl(DefService defService,
                                      InsService insService,
                                      TaskService taskService,
                                      WorkflowRuntimeQueryMapper queryMapper,
                                      WorkflowLoginUserSupport loginUserSupport,
                                      List<WorkflowStartContextProvider> startContextProviders,
                                      WorkflowDistributedLock distributedLock,
                                      WorkflowAfterCommitEventPublisher eventPublisher,
                                      PlatformTransactionManager transactionManager) {
        this.defService = defService;
        this.insService = insService;
        this.taskService = taskService;
        this.queryMapper = queryMapper;
        this.loginUserSupport = loginUserSupport;
        this.startContextProviders = startContextProviders;
        this.distributedLock = distributedLock;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public WorkflowStartResult start(WorkflowStartCommand command) {
        validateStartCommand(command);
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "start:" + command.getBusinessId();
        return distributedLock.execute(lockKey,
                () -> transactionTemplate.execute(status -> doStart(command)));
    }

    private WorkflowStartResult doStart(WorkflowStartCommand command) {
        if (queryMapper.selectInstanceByBusinessId(command.getBusinessId()) != null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID已存在，不能重复发起流程");
        }
        Definition definition = requirePublishedDefinition(command.getFlowCode());
        Long operatorId = currentUserId();
        Long deptId = loginUserSupport.getCurrentDeptId();
        Map<String, Object> variables = command.getVariables() == null
                ? new HashMap<>() : new HashMap<>(command.getVariables());
        variables.entrySet().removeIf(entry -> entry.getValue() == null);
        variables.put(WorkflowConstants.INITIATOR_ID_VARIABLE, operatorId);
        if (deptId != null) {
            variables.put(WorkflowConstants.INITIATOR_DEPT_ID_VARIABLE, deptId);
        }
        WorkflowStartContext context = new WorkflowStartContext(
                command.getFlowCode(), command.getBusinessId(), operatorId, deptId, Map.copyOf(variables));
        startContextProviders.stream()
                .filter(provider -> provider.supports(command.getFlowCode()))
                .map(provider -> provider.provide(context))
                .filter(Objects::nonNull)
                .forEach(variables::putAll);

        FlowParams flowParams = FlowParams.build()
                .flowCode(command.getFlowCode())
                .handler(String.valueOf(operatorId))
                .variable(variables)
                .flowStatus(FlowStatus.TOBESUBMIT.getKey());
        Instance instance = WorkflowEngineExceptionTranslator.execute(
                () -> insService.start(command.getBusinessId(), flowParams));
        Long taskId = queryMapper.selectCurrentTaskId(instance.getId());
        if (taskId == null) {
            throw new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "流程启动失败，未生成首个任务");
        }
        publishEvent(WorkflowOperation.START, definition, instance, taskId, operatorId,
                null, instance.getNodeCode());
        return toStartResult(definition, instance, taskId);
    }

    @Override
    public WorkflowInstanceSnapshot pass(WorkflowTaskCommand command) {
        validateTaskCommand(command);
        return executeTask(command.getTaskId(), task -> {
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.pass(command.getTaskId(), command.getMessage(), safeVariables(command.getVariables())));
            return buildOperationResult(task, instance, WorkflowOperation.PASS, command.getTaskId());
        });
    }

    @Override
    public WorkflowInstanceSnapshot rejectLast(WorkflowTaskCommand command) {
        validateTaskCommand(command);
        return executeTask(command.getTaskId(), task -> {
            boolean hasPreviousUserTask = queryMapper.selectHistoryByInstanceId(task.getInstanceId()).stream()
                    .anyMatch(history -> Integer.valueOf(1).equals(history.getNodeType())
                            && targetContains(history.getTargetNodeCode(), task.getNodeCode()));
            if (!hasPreviousUserTask) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "当前任务不存在可退回的上一人工节点");
            }
            FlowParams flowParams = FlowParams.build()
                    .message(command.getMessage())
                    .variable(safeVariables(command.getVariables()));
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.rejectLast(command.getTaskId(), flowParams));
            return buildOperationResult(task, instance, WorkflowOperation.REJECT_LAST, command.getTaskId());
        });
    }

    @Override
    public WorkflowInstanceSnapshot rejectTo(WorkflowRejectCommand command) {
        validateRejectCommand(command);
        return executeTask(command.getTaskId(), task -> {
            if (!targetContains(task.getAllowedRejectNodeCode(), command.getTargetNodeCode())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标节点不在当前流程定义允许的退回范围内");
            }
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.rejectAtWill(command.getTaskId(), command.getTargetNodeCode(),
                            command.getMessage(), safeVariables(command.getVariables())));
            return buildOperationResult(task, instance, WorkflowOperation.REJECT_TO, command.getTaskId());
        });
    }

    @Override
    public WorkflowInstanceSnapshot cancel(WorkflowCancelCommand command) {
        if (command == null || command.getInstanceId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程实例ID不能为空");
        }
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "instance:" + command.getInstanceId();
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowInstanceVo current = requireInstance(command.getInstanceId());
            Long operatorId = currentUserId();
            if (!String.valueOf(operatorId).equals(current.getInitiatorId())) {
                throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "只有流程发起人可以撤销流程");
            }
            if (Integer.valueOf(2).equals(current.getNodeType())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "已结束流程不能撤销");
            }
            Long taskId = queryMapper.selectCurrentTaskId(command.getInstanceId());
            FlowParams flowParams = FlowParams.build()
                    .handler(String.valueOf(operatorId))
                    .message(command.getMessage())
                    .flowStatus(FlowStatus.CANCEL.getKey())
                    .hisStatus(FlowStatus.CANCEL.getKey())
                    .ignore(true);
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.revoke(command.getInstanceId(), flowParams));
            Definition definition = requireDefinition(instance.getDefinitionId());
            Long currentTaskId = queryMapper.selectCurrentTaskId(instance.getId());
            publishEvent(WorkflowOperation.CANCEL, definition, instance, taskId, operatorId,
                    current.getNodeCode(), instance.getNodeCode());
            return toSnapshot(definition, instance, currentTaskId);
        }));
    }

    @Override
    public WorkflowInstanceSnapshot getByBusinessId(String businessId) {
        if (!StringUtils.hasText(businessId)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID不能为空");
        }
        WorkflowInstanceVo instance = queryMapper.selectInstanceByBusinessId(businessId);
        if (instance == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
        }
        return toSnapshot(instance, queryMapper.selectCurrentTaskId(instance.getInstanceId()));
    }

    @Override
    public PageResponse<WorkflowDefinitionVo> publishedDefinitions(WorkflowDefinitionSearchDto searchDto) {
        WorkflowDefinitionSearchDto query = searchDto == null ? new WorkflowDefinitionSearchDto() : searchDto;
        IPage<WorkflowDefinitionVo> page = queryMapper.selectDefinitionPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, true);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<WorkflowDefinitionVo> definitionPage(WorkflowDefinitionSearchDto searchDto) {
        WorkflowDefinitionSearchDto query = searchDto == null ? new WorkflowDefinitionSearchDto() : searchDto;
        IPage<WorkflowDefinitionVo> page = queryMapper.selectDefinitionPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, false);
        return toPageResponse(page);
    }

    @Override
    public WorkflowDefinitionVo definitionDetail(Long definitionId) {
        WorkflowDefinitionVo definition = queryMapper.selectDefinitionById(definitionId);
        if (definition == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程定义不存在");
        }
        return definition;
    }

    @Override
    public boolean publishDefinition(Long definitionId) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(() -> defService.publish(definitionId));
    }

    @Override
    public boolean unpublishDefinition(Long definitionId) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(() -> defService.unPublish(definitionId));
    }

    @Override
    public boolean copyDefinition(Long definitionId) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(() -> defService.copyDef(definitionId));
    }

    @Override
    public void deleteDefinition(Long definitionId) {
        requireDefinition(definitionId);
        if (queryMapper.countInstancesByDefinitionId(definitionId) > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义已产生实例，不能删除");
        }
        WorkflowEngineExceptionTranslator.execute(() -> defService.removeDef(List.of(definitionId)));
    }

    @Override
    public PageResponse<WorkflowInstanceVo> myInstances(WorkflowInstanceSearchDto searchDto) {
        WorkflowInstanceSearchDto query = searchDto == null ? new WorkflowInstanceSearchDto() : searchDto;
        IPage<WorkflowInstanceVo> page = queryMapper.selectInstancePage(
                new Page<>(query.getPageNum(), query.getPageSize()), query,
                String.valueOf(currentUserId()), null);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<WorkflowInstanceVo> runningInstances(WorkflowInstanceSearchDto searchDto) {
        return instancePage(searchDto, false);
    }

    @Override
    public PageResponse<WorkflowInstanceVo> finishedInstances(WorkflowInstanceSearchDto searchDto) {
        return instancePage(searchDto, true);
    }

    private PageResponse<WorkflowInstanceVo> instancePage(WorkflowInstanceSearchDto searchDto, boolean finished) {
        WorkflowInstanceSearchDto query = searchDto == null ? new WorkflowInstanceSearchDto() : searchDto;
        IPage<WorkflowInstanceVo> page = queryMapper.selectInstancePage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, null, finished);
        return toPageResponse(page);
    }

    @Override
    public WorkflowInstanceVo instanceDetail(Long instanceId, boolean administrator) {
        WorkflowInstanceVo instance = requireInstance(instanceId);
        if (!administrator) {
            requireInstanceAccess(instanceId);
        }
        return instance;
    }

    @Override
    public List<WorkflowHistoryVo> instanceHistory(Long instanceId, boolean administrator) {
        requireInstance(instanceId);
        if (!administrator) {
            requireInstanceAccess(instanceId);
        }
        return queryMapper.selectHistoryByInstanceId(instanceId);
    }

    @Override
    public PageResponse<WorkflowTaskVo> pendingTasks(WorkflowTaskSearchDto searchDto) {
        WorkflowTaskSearchDto query = searchDto == null ? new WorkflowTaskSearchDto() : searchDto;
        IPage<WorkflowTaskVo> page = queryMapper.selectPendingTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, currentPermissions());
        return toPageResponse(page);
    }

    @Override
    public PageResponse<WorkflowTaskVo> completedTasks(WorkflowTaskSearchDto searchDto) {
        WorkflowTaskSearchDto query = searchDto == null ? new WorkflowTaskSearchDto() : searchDto;
        IPage<WorkflowTaskVo> page = queryMapper.selectCompletedTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query,
                String.valueOf(currentUserId()));
        return toPageResponse(page);
    }

    @Override
    public WorkflowTaskVo taskDetail(Long taskId) {
        WorkflowTaskVo task = requireTask(taskId);
        requireTaskAccess(taskId);
        return task;
    }

    private WorkflowInstanceSnapshot executeTask(Long taskId, TaskOperation taskOperation) {
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "task:" + taskId;
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowTaskVo task = requireTask(taskId);
            requireTaskAccess(taskId);
            return taskOperation.execute(task);
        }));
    }

    private WorkflowInstanceSnapshot buildOperationResult(WorkflowTaskVo sourceTask,
                                                           Instance instance,
                                                           WorkflowOperation operation,
                                                           Long operatedTaskId) {
        Definition definition = requireDefinition(instance.getDefinitionId());
        Long currentTaskId = queryMapper.selectCurrentTaskId(instance.getId());
        publishEvent(operation, definition, instance, operatedTaskId, currentUserId(),
                sourceTask.getNodeCode(), instance.getNodeCode());
        return toSnapshot(definition, instance, currentTaskId);
    }

    private void requireInstanceAccess(Long instanceId) {
        long count = queryMapper.countInstanceAccess(instanceId, String.valueOf(currentUserId()), currentPermissions());
        if (count == 0) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "无权查看该流程实例");
        }
    }

    private WorkflowTaskVo requireTask(Long taskId) {
        if (taskId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID不能为空");
        }
        WorkflowTaskVo task = queryMapper.selectTaskById(taskId);
        if (task == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程任务不存在或已办理");
        }
        return task;
    }

    private void requireTaskAccess(Long taskId) {
        if (queryMapper.countTaskAccess(taskId, currentPermissions()) == 0) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "当前用户无权办理该任务");
        }
    }

    private Definition requirePublishedDefinition(String flowCode) {
        Definition definition = WorkflowEngineExceptionTranslator.execute(
                () -> defService.getPublishByFlowCode(flowCode));
        if (definition == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程定义未发布或不存在");
        }
        if (!ActivityStatus.ACTIVITY.getKey().equals(definition.getActivityStatus())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义已挂起");
        }
        return definition;
    }

    private Definition requireDefinition(Long definitionId) {
        if (definitionId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义ID不能为空");
        }
        Definition definition = WorkflowEngineExceptionTranslator.execute(() -> defService.getById(definitionId));
        if (definition == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程定义不存在");
        }
        return definition;
    }

    private WorkflowInstanceVo requireInstance(Long instanceId) {
        if (instanceId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程实例ID不能为空");
        }
        WorkflowInstanceVo instance = queryMapper.selectInstanceById(instanceId);
        if (instance == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
        }
        return instance;
    }

    private void publishEvent(WorkflowOperation operation,
                              Definition definition,
                              Instance instance,
                              Long taskId,
                              Long operatorId,
                              String sourceNodeCode,
                              String targetNodeCode) {
        eventPublisher.publish(new WorkflowChangedEvent(
                UUID.randomUUID(), Instant.now(), operation, definition.getFlowCode(),
                definition.getId(), instance.getId(), instance.getBusinessId(), taskId,
                operatorId, sourceNodeCode, targetNodeCode, instance.getFlowStatus()));
    }

    private WorkflowStartResult toStartResult(Definition definition, Instance instance, Long taskId) {
        WorkflowStartResult result = new WorkflowStartResult();
        result.setDefinitionId(definition.getId());
        result.setInstanceId(instance.getId());
        result.setTaskId(taskId);
        result.setBusinessId(instance.getBusinessId());
        result.setFlowCode(definition.getFlowCode());
        result.setFlowName(definition.getFlowName());
        result.setNodeCode(instance.getNodeCode());
        result.setNodeName(instance.getNodeName());
        result.setFlowStatus(instance.getFlowStatus());
        return result;
    }

    private WorkflowInstanceSnapshot toSnapshot(Definition definition, Instance instance, Long taskId) {
        WorkflowInstanceSnapshot snapshot = new WorkflowInstanceSnapshot();
        snapshot.setDefinitionId(definition.getId());
        snapshot.setInstanceId(instance.getId());
        snapshot.setTaskId(taskId);
        snapshot.setBusinessId(instance.getBusinessId());
        snapshot.setFlowCode(definition.getFlowCode());
        snapshot.setFlowName(definition.getFlowName());
        snapshot.setNodeCode(instance.getNodeCode());
        snapshot.setNodeName(instance.getNodeName());
        snapshot.setFlowStatus(instance.getFlowStatus());
        return snapshot;
    }

    private WorkflowInstanceSnapshot toSnapshot(WorkflowInstanceVo instance, Long taskId) {
        WorkflowInstanceSnapshot snapshot = new WorkflowInstanceSnapshot();
        snapshot.setDefinitionId(instance.getDefinitionId());
        snapshot.setInstanceId(instance.getInstanceId());
        snapshot.setTaskId(taskId);
        snapshot.setBusinessId(instance.getBusinessId());
        snapshot.setFlowCode(instance.getFlowCode());
        snapshot.setFlowName(instance.getFlowName());
        snapshot.setNodeCode(instance.getNodeCode());
        snapshot.setNodeName(instance.getNodeName());
        snapshot.setFlowStatus(instance.getFlowStatus());
        return snapshot;
    }

    private <T> PageResponse<T> toPageResponse(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal());
    }

    private Long currentUserId() {
        return LoginUserInfoModelContext.getRequiredLoginUserId();
    }

    private List<String> currentPermissions() {
        List<String> permissions = loginUserSupport.getCurrentPermissions();
        if (permissions.isEmpty()) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return permissions;
    }

    private Map<String, Object> safeVariables(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safeVariables = new HashMap<>(variables);
        safeVariables.entrySet().removeIf(entry -> entry.getValue() == null);
        return safeVariables;
    }

    private boolean targetContains(String allowedTargets, String targetNodeCode) {
        if (!StringUtils.hasText(allowedTargets) || !StringUtils.hasText(targetNodeCode)) {
            return false;
        }
        return Arrays.stream(allowedTargets.split(","))
                .map(String::trim)
                .anyMatch(targetNodeCode::equals);
    }

    private void validateStartCommand(WorkflowStartCommand command) {
        if (command == null || !StringUtils.hasText(command.getFlowCode())
                || !StringUtils.hasText(command.getBusinessId())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程编码和业务ID不能为空");
        }
        if (command.getBusinessId().length() > 40) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID长度不能超过40");
        }
    }

    private void validateTaskCommand(WorkflowTaskCommand command) {
        if (command == null || command.getTaskId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID不能为空");
        }
    }

    private void validateRejectCommand(WorkflowRejectCommand command) {
        if (command == null || command.getTaskId() == null
                || !StringUtils.hasText(command.getTargetNodeCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID和退回节点不能为空");
        }
    }

    @FunctionalInterface
    private interface TaskOperation {
        WorkflowInstanceSnapshot execute(WorkflowTaskVo task);
    }
}
