package com.vita.workflow.controller.impl;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.api.command.WorkflowCancelCommand;
import com.vita.workflow.api.command.WorkflowStartCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowStartResult;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.api.WorkflowInstanceControllerApi;
import com.vita.workflow.controller.dto.WorkflowCancelRequest;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.impl
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例接口默认实现
 * @Version: 1.0
 */
@Validated
public class DefaultWorkflowInstanceController implements WorkflowInstanceControllerApi {

    protected final WorkflowRuntimeService workflowRuntimeService;

    public DefaultWorkflowInstanceController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    /**
     * 创建待提交流程实例。
     *
     * @param command 流程启动命令
     * @return 流程启动结果
     */
    @Override
    public CommonResult<WorkflowStartResult> start(WorkflowStartCommand command) {
        return CommonResult.success(workflowRuntimeService.start(command));
    }

    /**
     * 分页查询当前用户发起的流程。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @Override
    public CommonResult<PageResponse<WorkflowInstanceVo>> mine(WorkflowInstanceSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.myInstances(searchDto));
    }

    /**
     * 查询当前用户有权访问的流程实例。
     *
     * @param instanceId 流程实例 ID
     * @return 流程实例详情
     */
    @Override
    public CommonResult<WorkflowInstanceVo> instanceDetail(Long instanceId) {
        return CommonResult.success(workflowRuntimeService.instanceDetail(instanceId, false));
    }

    /**
     * 查询当前用户有权访问的流程轨迹。
     *
     * @param instanceId 流程实例 ID
     * @return 流程轨迹
     */
    @Override
    public CommonResult<List<WorkflowHistoryVo>> instanceHistory(Long instanceId) {
        return CommonResult.success(workflowRuntimeService.instanceHistory(instanceId, false));
    }

    /**
     * 在审批人尚未办理时撤回流程。
     *
     * @param instanceId 流程实例 ID
     * @param request 撤回请求
     * @return 流程实例快照
     */
    @Override
    public CommonResult<WorkflowInstanceSnapshot> cancel(Long instanceId, WorkflowCancelRequest request) {
        WorkflowCancelCommand command = new WorkflowCancelCommand();
        command.setInstanceId(instanceId);
        command.setMessage(request.getMessage());
        return CommonResult.success(workflowRuntimeService.cancel(command));
    }

    /**
     * 管理端分页查询运行中的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowInstanceVo>> runningInstances(
            WorkflowInstanceSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.runningInstances(searchDto));
    }

    /**
     * 管理端分页查询已完成的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowInstanceVo>> finishedInstances(
            WorkflowInstanceSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.finishedInstances(searchDto));
    }

    /**
     * 管理端查询流程实例详情。
     *
     * @param instanceId 流程实例 ID
     * @return 流程实例详情
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<WorkflowInstanceVo> managedInstanceDetail(Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.instanceDetail(instanceId, true));
    }

    /**
     * 管理端查询流程轨迹。
     *
     * @param instanceId 流程实例 ID
     * @return 流程轨迹
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<List<WorkflowHistoryVo>> managedInstanceHistory(Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.instanceHistory(instanceId, true));
    }
}
