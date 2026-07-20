package com.vita.controller.workflow;

import com.vita.controller.workflow.dto.WorkflowActionRequest;
import com.vita.controller.workflow.dto.WorkflowRejectToRequest;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.api.command.WorkflowRejectCommand;
import com.vita.workflow.api.command.WorkflowTaskCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowTaskVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 普通用户流程任务接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/workflow/tasks")
@ConditionalOnWorkflowEnabled
public class WorkflowRuntimeTaskController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowRuntimeTaskController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    @GetMapping("/pending")
    public CommonResult<PageResponse<WorkflowTaskVo>> pending(@Valid WorkflowTaskSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.pendingTasks(searchDto));
    }

    @GetMapping("/completed")
    public CommonResult<PageResponse<WorkflowTaskVo>> completed(@Valid WorkflowTaskSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.completedTasks(searchDto));
    }

    @GetMapping("/{taskId}")
    public CommonResult<WorkflowTaskVo> detail(@PathVariable Long taskId) {
        return CommonResult.success(workflowRuntimeService.taskDetail(taskId));
    }

    @PostMapping("/{taskId}/pass")
    public CommonResult<WorkflowInstanceSnapshot> pass(@PathVariable Long taskId,
                                                        @Valid @RequestBody WorkflowActionRequest request) {
        return CommonResult.success(workflowRuntimeService.pass(toTaskCommand(taskId, request)));
    }

    @PostMapping("/{taskId}/reject-last")
    public CommonResult<WorkflowInstanceSnapshot> rejectLast(@PathVariable Long taskId,
                                                              @Valid @RequestBody WorkflowActionRequest request) {
        return CommonResult.success(workflowRuntimeService.rejectLast(toTaskCommand(taskId, request)));
    }

    @PostMapping("/{taskId}/reject-to")
    public CommonResult<WorkflowInstanceSnapshot> rejectTo(@PathVariable Long taskId,
                                                            @Valid @RequestBody WorkflowRejectToRequest request) {
        WorkflowRejectCommand command = new WorkflowRejectCommand();
        command.setTaskId(taskId);
        command.setTargetNodeCode(request.getTargetNodeCode());
        command.setMessage(request.getMessage());
        command.setVariables(request.getVariables());
        return CommonResult.success(workflowRuntimeService.rejectTo(command));
    }

    private WorkflowTaskCommand toTaskCommand(Long taskId, WorkflowActionRequest request) {
        WorkflowTaskCommand command = new WorkflowTaskCommand();
        command.setTaskId(taskId);
        command.setMessage(request.getMessage());
        command.setVariables(request.getVariables());
        return command;
    }
}
