package com.vita.workflow.controller.api;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.workflow.api.command.WorkflowStartCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowStartResult;
import com.vita.workflow.controller.dto.WorkflowCancelRequest;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.api
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例 HTTP 接口契约
 * @Version: 1.0
 */
@RestController
public interface WorkflowInstanceControllerApi {

    /**
     * 创建待提交流程实例。
     *
     * @param command 流程启动命令
     * @return 流程启动结果
     */
    @PostMapping("/workflow/instances/start")
    CommonResult<WorkflowStartResult> start(@Valid @RequestBody WorkflowStartCommand command);

    /**
     * 分页查询当前用户发起的流程。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @GetMapping("/workflow/instances/mine")
    CommonResult<PageResponse<WorkflowInstanceVo>> mine(@Valid WorkflowInstanceSearchDto searchDto);

    /**
     * 查询当前用户有权访问的流程实例。
     *
     * @param instanceId 流程实例 ID
     * @return 流程实例详情
     */
    @GetMapping("/workflow/instances/{instanceId}")
    CommonResult<WorkflowInstanceVo> instanceDetail(@PathVariable("instanceId") Long instanceId);

    /**
     * 查询当前用户有权访问的流程轨迹。
     *
     * @param instanceId 流程实例 ID
     * @return 流程轨迹
     */
    @GetMapping("/workflow/instances/{instanceId}/history")
    CommonResult<List<WorkflowHistoryVo>> instanceHistory(@PathVariable("instanceId") Long instanceId);

    /**
     * 在审批人尚未办理时撤回流程。
     *
     * @param instanceId 流程实例 ID
     * @param request 撤回请求
     * @return 流程实例快照
     */
    @PostMapping("/workflow/instances/{instanceId}/cancel")
    CommonResult<WorkflowInstanceSnapshot> cancel(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody WorkflowCancelRequest request);

    /**
     * 管理端分页查询运行中的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @GetMapping("/system/workflow/instances/running")
    CommonResult<PageResponse<WorkflowInstanceVo>> runningInstances(
            @Valid WorkflowInstanceSearchDto searchDto);

    /**
     * 管理端分页查询已完成的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @GetMapping("/system/workflow/instances/finished")
    CommonResult<PageResponse<WorkflowInstanceVo>> finishedInstances(
            @Valid WorkflowInstanceSearchDto searchDto);

    /**
     * 管理端查询流程实例详情。
     *
     * @param instanceId 流程实例 ID
     * @return 流程实例详情
     */
    @GetMapping("/system/workflow/instances/{instanceId}")
    CommonResult<WorkflowInstanceVo> managedInstanceDetail(
            @PathVariable("instanceId") Long instanceId);

    /**
     * 管理端查询流程轨迹。
     *
     * @param instanceId 流程实例 ID
     * @return 流程轨迹
     */
    @GetMapping("/system/workflow/instances/{instanceId}/history")
    CommonResult<List<WorkflowHistoryVo>> managedInstanceHistory(
            @PathVariable("instanceId") Long instanceId);
}
