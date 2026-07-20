package com.vita.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流实例只读监控接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/system/workflow/instances")
@ConditionalOnWorkflowEnabled
public class WorkflowInstanceAdminController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowInstanceAdminController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    @GetMapping("/running")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowInstanceVo>> running(@Valid WorkflowInstanceSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.runningInstances(searchDto));
    }

    @GetMapping("/finished")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowInstanceVo>> finished(@Valid WorkflowInstanceSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.finishedInstances(searchDto));
    }

    @GetMapping("/{instanceId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<WorkflowInstanceVo> detail(@PathVariable Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.instanceDetail(instanceId, true));
    }

    @GetMapping("/{instanceId}/history")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<List<WorkflowHistoryVo>> history(@PathVariable Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.instanceHistory(instanceId, true));
    }
}
