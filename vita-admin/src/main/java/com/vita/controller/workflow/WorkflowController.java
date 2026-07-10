package com.vita.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.service.IWorkflowDefinitionService;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作流后台控制器。
 *
 * @author Codex
 */
@RestController
@RequestMapping("/system/workflow")
public class WorkflowController extends BaseController {

    private final IWorkflowDefinitionService workflowDefinitionService;

    public WorkflowController(IWorkflowDefinitionService workflowDefinitionService) {
        this.workflowDefinitionService = workflowDefinitionService;
    }

    /**
     * 导入 Warm-Flow 流程定义 JSON。
     *
     * @param defJson 流程定义对象
     * @return 导入结果
     */
    @RepeatSubmit
    @PostMapping(value = "/importJson", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SaCheckPermission(WorkflowConstants.WORKFLOW_IMPORT_PERMISSION)
    public CommonResult<Boolean> importJson(@RequestBody(required = false) DefJson defJson) {
        checkWorkflowCreatorRole();
        return CommonResult.success(workflowDefinitionService.importJson(defJson));
    }

    private void checkWorkflowCreatorRole() {
        try {
            StpUtil.checkLogin();
        } catch (Exception ex) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        if (StpUtil.hasRole(AuthConstants.SUPER_ADMIN_ROLE_CODE) || StpUtil.hasRole(AuthConstants.SUB_ADMIN_ROLE_CODE)) {
            return;
        }
        throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "只有子管理员或超级管理员可以创建流程");
    }
}
