package com.vita.controller.workflow;

import com.vita.controller.workflow.dto.WorkflowCancelRequest;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.api.command.WorkflowCancelCommand;
import com.vita.workflow.api.command.WorkflowStartCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowStartResult;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 普通用户流程实例接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/workflow/instances")
@ConditionalOnWorkflowEnabled
public class WorkflowRuntimeInstanceController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowRuntimeInstanceController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    @PostMapping("/start")
    public CommonResult<WorkflowStartResult> start(@Valid @RequestBody WorkflowStartCommand command) {
        return CommonResult.success(workflowRuntimeService.start(command));
    }

    @GetMapping("/mine")
    public CommonResult<PageResponse<WorkflowInstanceVo>> mine(@Valid WorkflowInstanceSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.myInstances(searchDto));
    }

    @GetMapping("/{instanceId}")
    public CommonResult<WorkflowInstanceVo> detail(@PathVariable Long instanceId) {
        return CommonResult.success(workflowRuntimeService.instanceDetail(instanceId, false));
    }

    @GetMapping("/{instanceId}/history")
    public CommonResult<List<WorkflowHistoryVo>> history(@PathVariable Long instanceId) {
        return CommonResult.success(workflowRuntimeService.instanceHistory(instanceId, false));
    }

    @PostMapping("/{instanceId}/cancel")
    public CommonResult<WorkflowInstanceSnapshot> cancel(@PathVariable Long instanceId,
                                                          @Valid @RequestBody WorkflowCancelRequest request) {
        WorkflowCancelCommand command = new WorkflowCancelCommand();
        command.setInstanceId(instanceId);
        command.setMessage(request.getMessage());
        return CommonResult.success(workflowRuntimeService.cancel(command));
    }
}
