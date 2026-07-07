package com.vita.workflow.support;

import cn.dev33.satoken.stp.StpUtil;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.model.LoginUserInfoModel;
import com.vita.workflow.constant.WorkflowConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
     * 同时返回用户、角色与当前部门标识，供 Warm-Flow 校验节点办理权限。
     *
     * @return 权限集合
     */
    public List<String> getCurrentPermissions() {
        Long userId = LoginUserInfoModelContext.getLoginUserId();
        if (userId == null) {
            return List.of();
        }
        Set<String> permissions = new LinkedHashSet<>();
        permissions.add(WorkflowConstants.buildUserPermission(userId));
        for (String roleCode : getCurrentRoleCodes()) {
            String rolePermission = WorkflowConstants.buildRolePermission(roleCode);
            if (rolePermission != null) {
                permissions.add(rolePermission);
            }
        }
        String deptPermission = WorkflowConstants.buildDeptPermission(getCurrentDeptId());
        if (deptPermission != null) {
            permissions.add(deptPermission);
        }
        return new ArrayList<>(permissions);
    }

    /**
     * 获取当前登录用户的角色编码集合。
     *
     * @return 角色编码集合
     */
    public List<String> getCurrentRoleCodes() {
        try {
            List<String> roleCodes = StpUtil.getRoleList();
            return roleCodes == null ? List.of() : roleCodes;
        } catch (Exception ex) {
            return List.of();
        }
    }

    /**
     * 获取当前登录用户部门 ID。
     *
     * @return 当前部门 ID
     */
    public Long getCurrentDeptId() {
        LoginUserInfoModel loginUserInfo = LoginUserInfoModelContext.getLoginUserInfo();
        return loginUserInfo == null ? null : loginUserInfo.getDeptId();
    }
}
