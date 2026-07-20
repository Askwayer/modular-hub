package com.vita.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流定义管理接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/system/workflow/definitions")
@ConditionalOnWorkflowEnabled
public class WorkflowDefinitionAdminController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowDefinitionAdminController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    @GetMapping("/page")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowDefinitionVo>> page(@Valid WorkflowDefinitionSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.definitionPage(searchDto));
    }

    @GetMapping("/{definitionId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
    public CommonResult<WorkflowDefinitionVo> detail(@PathVariable Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.definitionDetail(definitionId));
    }

    @PutMapping("/{definitionId}/publish")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> publish(@PathVariable Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.publishDefinition(definitionId));
    }

    @PutMapping("/{definitionId}/unpublish")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> unpublish(@PathVariable Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.unpublishDefinition(definitionId));
    }

    @PostMapping("/{definitionId}/copy")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> copy(@PathVariable Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.copyDefinition(definitionId));
    }

    @DeleteMapping("/{definitionId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> delete(@PathVariable Long definitionId) {
        WorkflowAdminAccessChecker.check();
        workflowRuntimeService.deleteDefinition(definitionId);
        return CommonResult.success(true);
    }
}
