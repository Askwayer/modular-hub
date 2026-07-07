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
     * 报销审批流程定义资源路径。
     */
    public static final String REIMBURSE_APPROVAL_V1_RESOURCE = "workflow/definitions/reimburse-approval-v1.json";

    /**
     * 办理人选择器中的角色页签名称。
     */
    public static final String ROLE_HANDLER_TYPE = "角色";

    /**
     * 用户权限标识前缀。
     */
    public static final String USER_PERMISSION_PREFIX = "user:";

    /**
     * 角色权限标识前缀。
     */
    public static final String ROLE_PERMISSION_PREFIX = "role:";

    /**
     * 财务审批角色编码。
     */
    public static final String FINANCE_APPROVER_ROLE_CODE = "FINANCE_APPROVER";

    /**
     * 综合管理部审批角色编码。
     */
    public static final String GENERAL_AFFAIRS_APPROVER_ROLE_CODE = "GENERAL_AFFAIRS_APPROVER";

    /**
     * 总经理审批角色编码。
     */
    public static final String GENERAL_MANAGER_APPROVER_ROLE_CODE = "GENERAL_MANAGER_APPROVER";

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
}
