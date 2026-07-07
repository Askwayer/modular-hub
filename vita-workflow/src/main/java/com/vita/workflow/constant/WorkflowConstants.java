package com.vita.workflow.constant;

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
     * 历史物理超管角色编码，仅用于兼容旧流程定义回显。
     */
    public static final String LEGACY_SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN_ROLE";

    /**
     * 设计器中的超级管理员显示名称。
     */
    public static final String SUPER_ADMIN_HANDLER_NAME = "超级管理员";

    /**
     * 部门权限标识前缀。
     */
    public static final String DEPT_PERMISSION_PREFIX = "dept:";

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
