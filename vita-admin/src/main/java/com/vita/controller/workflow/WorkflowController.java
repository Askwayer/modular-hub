package com.vita.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
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
@ConditionalOnWorkflowEnabled
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
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowDefinitionService.importJson(defJson));
    }
}
