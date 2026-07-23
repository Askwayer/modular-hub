package com.vita.workflow.runtime.service;

import com.vita.core.page.PageResponse;
import com.vita.workflow.api.WorkflowPort;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import com.vita.workflow.runtime.vo.WorkflowTaskVo;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.service
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流运行时查询与管理服务
 * @Version: 1.0
 */
public interface WorkflowRuntimeService extends WorkflowPort {

    /**
     * 分页查询可发起的已发布流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    PageResponse<WorkflowDefinitionVo> publishedDefinitions(WorkflowDefinitionSearchDto searchDto);

    /**
     * 管理端分页查询全部流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    PageResponse<WorkflowDefinitionVo> definitionPage(WorkflowDefinitionSearchDto searchDto);

    /**
     * 查询流程定义详情。
     *
     * @param definitionId 流程定义 ID
     * @return 流程定义详情
     */
    WorkflowDefinitionVo definitionDetail(Long definitionId);

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 发布结果
     */
    boolean publishDefinition(Long definitionId);

    /**
     * 取消发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 取消发布结果
     */
    boolean unpublishDefinition(Long definitionId);

    /**
     * 复制流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 复制结果
     */
    boolean copyDefinition(Long definitionId);

    /**
     * 删除尚未产生实例的流程定义。
     *
     * @param definitionId 流程定义 ID
     */
    void deleteDefinition(Long definitionId);

    /**
     * 分页查询当前用户发起的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    PageResponse<WorkflowInstanceVo> myInstances(WorkflowInstanceSearchDto searchDto);

    /**
     * 管理端分页查询运行中的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    PageResponse<WorkflowInstanceVo> runningInstances(WorkflowInstanceSearchDto searchDto);

    /**
     * 管理端分页查询已完成的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    PageResponse<WorkflowInstanceVo> finishedInstances(WorkflowInstanceSearchDto searchDto);

    /**
     * 查询流程实例详情。
     *
     * @param instanceId 流程实例 ID
     * @param administrator 是否按管理员身份跳过参与人校验
     * @return 流程实例详情
     */
    WorkflowInstanceVo instanceDetail(Long instanceId, boolean administrator);

    /**
     * 查询流程实例轨迹。
     *
     * @param instanceId 流程实例 ID
     * @param administrator 是否按管理员身份跳过参与人校验
     * @return 流程轨迹
     */
    List<WorkflowHistoryVo> instanceHistory(Long instanceId, boolean administrator);

    /**
     * 分页查询当前用户待办任务。
     *
     * @param searchDto 查询条件
     * @return 待办任务分页结果
     */
    PageResponse<WorkflowTaskVo> pendingTasks(WorkflowTaskSearchDto searchDto);

    /**
     * 分页查询当前用户已办任务。
     *
     * @param searchDto 查询条件
     * @return 已办任务分页结果
     */
    PageResponse<WorkflowTaskVo> completedTasks(WorkflowTaskSearchDto searchDto);

    /**
     * 查询当前用户可办理的任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    WorkflowTaskVo taskDetail(Long taskId);
}
