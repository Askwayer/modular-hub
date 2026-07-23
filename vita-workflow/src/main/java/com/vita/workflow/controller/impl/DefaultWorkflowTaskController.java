package com.vita.workflow.controller.impl;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.api.command.WorkflowRejectCommand;
import com.vita.workflow.api.command.WorkflowTaskCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.controller.api.WorkflowTaskControllerApi;
import com.vita.workflow.controller.dto.WorkflowActionRequest;
import com.vita.workflow.controller.dto.WorkflowRejectToRequest;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowTaskVo;
import org.springframework.validation.annotation.Validated;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.impl
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流任务接口默认实现
 * @Version: 1.0
 */
@Validated
public class DefaultWorkflowTaskController implements WorkflowTaskControllerApi {

    protected final WorkflowRuntimeService workflowRuntimeService;

    public DefaultWorkflowTaskController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    /**
     * 分页查询当前用户待办任务。
     *
     * @param searchDto 查询条件
     * @return 待办任务分页结果
     */
    @Override
    public CommonResult<PageResponse<WorkflowTaskVo>> pending(WorkflowTaskSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.pendingTasks(searchDto));
    }

    /**
     * 分页查询当前用户已办任务。
     *
     * @param searchDto 查询条件
     * @return 已办任务分页结果
     */
    @Override
    public CommonResult<PageResponse<WorkflowTaskVo>> completed(WorkflowTaskSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.completedTasks(searchDto));
    }

    /**
     * 查询当前用户可办理的任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    @Override
    public CommonResult<WorkflowTaskVo> detail(Long taskId) {
        return CommonResult.success(workflowRuntimeService.taskDetail(taskId));
    }

    /**
     * 通过当前任务。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 流程实例快照
     */
    @Override
    public CommonResult<WorkflowInstanceSnapshot> pass(Long taskId, WorkflowActionRequest request) {
        return CommonResult.success(workflowRuntimeService.pass(toTaskCommand(taskId, request)));
    }

    /**
     * 退回上一人工节点。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 流程实例快照
     */
    @Override
    public CommonResult<WorkflowInstanceSnapshot> rejectLast(Long taskId, WorkflowActionRequest request) {
        return CommonResult.success(workflowRuntimeService.rejectLast(toTaskCommand(taskId, request)));
    }

    /**
     * 退回定义允许的指定节点。
     *
     * @param taskId 任务 ID
     * @param request 指定节点退回请求
     * @return 流程实例快照
     */
    @Override
    public CommonResult<WorkflowInstanceSnapshot> rejectTo(Long taskId, WorkflowRejectToRequest request) {
        WorkflowRejectCommand command = new WorkflowRejectCommand();
        command.setTaskId(taskId);
        command.setTargetNodeCode(request.getTargetNodeCode());
        command.setMessage(request.getMessage());
        command.setVariables(request.getVariables());
        return CommonResult.success(workflowRuntimeService.rejectTo(command));
    }

    /**
     * 将 HTTP 办理请求转换为引擎中立任务命令。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 任务命令
     */
    protected WorkflowTaskCommand toTaskCommand(Long taskId, WorkflowActionRequest request) {
        WorkflowTaskCommand command = new WorkflowTaskCommand();
        command.setTaskId(taskId);
        command.setMessage(request.getMessage());
        command.setVariables(request.getVariables());
        return command;
    }
}
