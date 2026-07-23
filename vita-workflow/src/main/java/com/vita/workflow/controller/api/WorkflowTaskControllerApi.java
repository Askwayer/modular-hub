package com.vita.workflow.controller.api;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.controller.dto.WorkflowActionRequest;
import com.vita.workflow.controller.dto.WorkflowRejectToRequest;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.vo.WorkflowTaskVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.api
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流任务 HTTP 接口契约
 * @Version: 1.0
 */
@RestController
public interface WorkflowTaskControllerApi {

    /**
     * 分页查询当前用户待办任务。
     *
     * @param searchDto 查询条件
     * @return 待办任务分页结果
     */
    @GetMapping("/workflow/tasks/pending")
    CommonResult<PageResponse<WorkflowTaskVo>> pending(@Valid WorkflowTaskSearchDto searchDto);

    /**
     * 分页查询当前用户已办任务。
     *
     * @param searchDto 查询条件
     * @return 已办任务分页结果
     */
    @GetMapping("/workflow/tasks/completed")
    CommonResult<PageResponse<WorkflowTaskVo>> completed(@Valid WorkflowTaskSearchDto searchDto);

    /**
     * 查询当前用户可办理的任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    @GetMapping("/workflow/tasks/{taskId}")
    CommonResult<WorkflowTaskVo> detail(@PathVariable("taskId") Long taskId);

    /**
     * 通过当前任务。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 流程实例快照
     */
    @PostMapping("/workflow/tasks/{taskId}/pass")
    CommonResult<WorkflowInstanceSnapshot> pass(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowActionRequest request);

    /**
     * 退回上一人工节点。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 流程实例快照
     */
    @PostMapping("/workflow/tasks/{taskId}/reject-last")
    CommonResult<WorkflowInstanceSnapshot> rejectLast(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowActionRequest request);

    /**
     * 退回定义允许的指定节点。
     *
     * @param taskId 任务 ID
     * @param request 指定节点退回请求
     * @return 流程实例快照
     */
    @PostMapping("/workflow/tasks/{taskId}/reject-to")
    CommonResult<WorkflowInstanceSnapshot> rejectTo(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowRejectToRequest request);
}
