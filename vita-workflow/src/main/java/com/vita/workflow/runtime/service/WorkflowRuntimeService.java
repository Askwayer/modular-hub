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

    PageResponse<WorkflowDefinitionVo> publishedDefinitions(WorkflowDefinitionSearchDto searchDto);

    PageResponse<WorkflowDefinitionVo> definitionPage(WorkflowDefinitionSearchDto searchDto);

    WorkflowDefinitionVo definitionDetail(Long definitionId);

    boolean publishDefinition(Long definitionId);

    boolean unpublishDefinition(Long definitionId);

    boolean copyDefinition(Long definitionId);

    void deleteDefinition(Long definitionId);

    PageResponse<WorkflowInstanceVo> myInstances(WorkflowInstanceSearchDto searchDto);

    PageResponse<WorkflowInstanceVo> runningInstances(WorkflowInstanceSearchDto searchDto);

    PageResponse<WorkflowInstanceVo> finishedInstances(WorkflowInstanceSearchDto searchDto);

    WorkflowInstanceVo instanceDetail(Long instanceId, boolean administrator);

    List<WorkflowHistoryVo> instanceHistory(Long instanceId, boolean administrator);

    PageResponse<WorkflowTaskVo> pendingTasks(WorkflowTaskSearchDto searchDto);

    PageResponse<WorkflowTaskVo> completedTasks(WorkflowTaskSearchDto searchDto);

    WorkflowTaskVo taskDetail(Long taskId);
}
