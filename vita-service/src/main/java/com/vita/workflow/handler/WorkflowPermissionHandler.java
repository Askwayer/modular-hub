package com.vita.workflow.handler;

import com.vita.workflow.support.WorkflowLoginUserSupport;
import org.dromara.warm.flow.core.handler.PermissionHandler;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.handler
 * @Author: Codex
 * @CreateTime: 2026-07-02
 * @Description: Warm-Flow 办理人权限处理器
 * @Version: 1.0
 */
public class WorkflowPermissionHandler implements PermissionHandler {

    private final WorkflowLoginUserSupport workflowLoginUserSupport;

    public WorkflowPermissionHandler(WorkflowLoginUserSupport workflowLoginUserSupport) {
        this.workflowLoginUserSupport = workflowLoginUserSupport;
    }

    /**
     * 一期只返回当前登录用户标识，供流程办理人与校验使用。
     */
    @Override
    public List<String> permissions() {
        return workflowLoginUserSupport.getCurrentPermissions();
    }

    /**
     * 返回当前办理人唯一标识。
     */
    @Override
    public String getHandler() {
        return workflowLoginUserSupport.getCurrentHandler();
    }
}
