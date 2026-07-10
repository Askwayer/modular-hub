package com.vita.workflow.constant;

import com.vita.auth.constant.AuthConstants;

import java.util.List;

/**
 * 工作流模块常量。
 *
 * @author Codex
 */
public final class WorkflowConstants {

    /**
     * 工作流 JSON 导入权限码。
     */
    public static final String WORKFLOW_IMPORT_PERMISSION = "system:workflow:import";

    /**
     * Warm-Flow 保存流程定义路径。
     */
    public static final String WARM_FLOW_SAVE_JSON_PATH = "/warm-flow/save-json";

    /**
     * 办理人选择器中的用户页签名称。
     */
    public static final String USER_HANDLER_TYPE = "用户";

    /**
     * 办理人选择器中的角色页签名称。
     */
    public static final String ROLE_HANDLER_TYPE = "角色";

    /**
     * 办理人选择器中的部门页签名称。
     */
    public static final String DEPT_HANDLER_TYPE = "部门";

    /**
     * 用户权限标识前缀。
     */
    public static final String USER_PERMISSION_PREFIX = "user:";

    /**
     * 角色权限标识前缀。
     */
    public static final String ROLE_PERMISSION_PREFIX = "role:";

    /**
     * 部门权限标识前缀。
     */
    public static final String DEPT_PERMISSION_PREFIX = "dept:";

    /**
     * 节点权限标识分隔符。
     */
    public static final String PERMISSION_FLAG_SEPARATOR = "@@";

    /**
     * 人工节点类型。
     */
    public static final int USER_TASK_NODE_TYPE = 1;

    /**
     * 历史物理超管角色编码，仅用于兼容旧流程定义回显。
     */
    public static final String LEGACY_SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN_ROLE";

    /**
     * 设计器中的超级管理员显示名称。
     */
    public static final String SUPER_ADMIN_HANDLER_NAME = "超级管理员";

    /**
     * 子管理员显示名称。
     */
    public static final String SUB_ADMIN_HANDLER_NAME = "子管理员";

    /**
     * 默认补齐到人工节点的管理员角色权限标识。
     */
    public static final List<String> DEFAULT_ADMIN_ROLE_PERMISSIONS = List.of(
            buildRolePermission(AuthConstants.SUPER_ADMIN_ROLE_CODE),
            buildRolePermission(AuthConstants.SUB_ADMIN_ROLE_CODE)
    );

    private WorkflowConstants() {
        // 工具类不允许实例化
    }

    /**
     * 构建用户权限标识。
     *
     * @param userId 用户 ID
     * @return 用户权限标识
     */
    public static String buildUserPermission(Long userId) {
        return userId == null ? null : USER_PERMISSION_PREFIX + userId;
    }

    /**
     * 构建角色权限标识。
     *
     * @param roleCode 角色编码
     * @return 角色权限标识
     */
    public static String buildRolePermission(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return ROLE_PERMISSION_PREFIX + roleCode;
    }

    /**
     * 构建部门权限标识。
     *
     * @param deptId 部门 ID
     * @return 部门权限标识
     */
    public static String buildDeptPermission(Long deptId) {
        return deptId == null ? null : DEPT_PERMISSION_PREFIX + deptId;
    }
}
