package com.vita.controller.workflow;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 普通用户可用的流程定义查询接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/workflow/definitions")
@ConditionalOnWorkflowEnabled
public class WorkflowRuntimeDefinitionController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowRuntimeDefinitionController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    /**
     * 分页查询可发起的已发布流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    @GetMapping("/published")
    public CommonResult<PageResponse<WorkflowDefinitionVo>> published(
            @Valid WorkflowDefinitionSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.publishedDefinitions(searchDto));
    }
}
