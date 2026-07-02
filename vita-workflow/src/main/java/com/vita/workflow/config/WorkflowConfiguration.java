package com.vita.workflow.config;

import com.vita.workflow.handler.WorkflowPermissionHandler;
import com.vita.workflow.support.WorkflowLoginUserSupport;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.config
 * @Author: Codex
 * @CreateTime: 2026-07-02
 * @Description: Warm-Flow 一期配置
 * @Version: 1.0
 */
@Configuration
public class WorkflowConfiguration {

    /**
     * 注册当前办理人权限处理器，复用 be-vita 登录上下文。
     */
    @Bean
    public PermissionHandler workflowPermissionHandler(WorkflowLoginUserSupport workflowLoginUserSupport) {
        return new WorkflowPermissionHandler(workflowLoginUserSupport);
    }
}
