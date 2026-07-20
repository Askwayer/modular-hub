package com.vita.workflow.api;

import com.vita.workflow.api.command.WorkflowCancelCommand;
import com.vita.workflow.api.command.WorkflowRejectCommand;
import com.vita.workflow.api.command.WorkflowStartCommand;
import com.vita.workflow.api.command.WorkflowTaskCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowStartResult;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 与具体工作流引擎无关的工作流操作端口
 * @Version: 1.0
 */
public interface WorkflowPort {

    /**
     * 创建流程实例。
     *
     * @param command 启动命令
     * @return 启动结果
     */
    WorkflowStartResult start(WorkflowStartCommand command);

    /**
     * 通过当前任务。
     *
     * @param command 任务命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot pass(WorkflowTaskCommand command);

    /**
     * 退回上一人工节点。
     *
     * @param command 任务命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot rejectLast(WorkflowTaskCommand command);

    /**
     * 退回指定节点。
     *
     * @param command 退回命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot rejectTo(WorkflowRejectCommand command);

    /**
     * 撤销流程实例。
     *
     * @param command 撤销命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot cancel(WorkflowCancelCommand command);

    /**
     * 根据全局业务 ID 查询实例。
     *
     * @param businessId 全局业务 ID
     * @return 实例快照
     */
    WorkflowInstanceSnapshot getByBusinessId(String businessId);
}
