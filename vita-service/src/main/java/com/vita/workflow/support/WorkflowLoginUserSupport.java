package com.vita.workflow.support;

import com.vita.core.context.LoginUserInfoModelContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.support
 * @Author: Codex
 * @CreateTime: 2026-07-02
 * @Description: 工作流当前登录用户支持
 * @Version: 1.0
 */
@Component
public class WorkflowLoginUserSupport {

    /**
     * 获取当前办理人唯一标识。
     *
     * @return 当前登录用户 ID 字符串
     */
    public String getCurrentHandler() {
        Long userId = LoginUserInfoModelContext.getLoginUserId();
        return userId == null ? null : String.valueOf(userId);
    }

    /**
     * 获取当前办理人权限集合。
     * 一期仅支持直接用户标识。
     *
     * @return 权限集合
     */
    public List<String> getCurrentPermissions() {
        String handler = getCurrentHandler();
        if (handler == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(handler);
    }
}
