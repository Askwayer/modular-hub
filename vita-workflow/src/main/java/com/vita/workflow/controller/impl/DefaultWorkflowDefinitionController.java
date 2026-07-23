package com.vita.workflow.controller.impl;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.api.WorkflowDefinitionControllerApi;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import com.vita.workflow.service.IWorkflowDefinitionService;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.validation.annotation.Validated;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.impl
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流定义接口默认实现
 * @Version: 1.0
 */
@Validated
public class DefaultWorkflowDefinitionController implements WorkflowDefinitionControllerApi {

    protected final WorkflowRuntimeService workflowRuntimeService;

    protected final IWorkflowDefinitionService workflowDefinitionService;

    public DefaultWorkflowDefinitionController(WorkflowRuntimeService workflowRuntimeService,
                                               IWorkflowDefinitionService workflowDefinitionService) {
        this.workflowRuntimeService = workflowRuntimeService;
        this.workflowDefinitionService = workflowDefinitionService;
    }

    /**
     * 分页查询已发布流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    @Override
    public CommonResult<PageResponse<WorkflowDefinitionVo>> published(
            WorkflowDefinitionSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.publishedDefinitions(searchDto));
    }

    /**
     * 管理端分页查询流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowDefinitionVo>> definitionPage(
            WorkflowDefinitionSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.definitionPage(searchDto));
    }

    /**
     * 管理端查询流程定义详情。
     *
     * @param definitionId 流程定义 ID
     * @return 流程定义详情
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
    public CommonResult<WorkflowDefinitionVo> definitionDetail(Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.definitionDetail(definitionId));
    }

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 发布结果
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> publishDefinition(Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.publishDefinition(definitionId));
    }

    /**
     * 取消发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 取消发布结果
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> unpublishDefinition(Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.unpublishDefinition(definitionId));
    }

    /**
     * 复制流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 复制结果
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> copyDefinition(Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.copyDefinition(definitionId));
    }

    /**
     * 安全删除流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 删除结果
     */
    @Override
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> deleteDefinition(Long definitionId) {
        WorkflowAdminAccessChecker.check();
        workflowRuntimeService.deleteDefinition(definitionId);
        return CommonResult.success(true);
    }

    /**
     * 导入 Warm-Flow 流程定义 JSON。
     *
     * @param defJson 流程定义对象
     * @return 导入结果
     */
    @Override
    @RepeatSubmit
    @SaCheckPermission(WorkflowConstants.WORKFLOW_IMPORT_PERMISSION)
    public CommonResult<Boolean> importJson(DefJson defJson) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowDefinitionService.importJson(defJson));
    }
}
