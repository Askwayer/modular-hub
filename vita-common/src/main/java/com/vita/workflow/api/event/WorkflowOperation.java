package com.vita.workflow.api.event;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.event
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流状态变更操作类型
 * @Version: 1.0
 */
public enum WorkflowOperation {
    START,
    PASS,
    REJECT_LAST,
    REJECT_TO,
    CANCEL
}
