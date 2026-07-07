-- =============================================
-- File: 20260707_sub_admin_workflow_create_upgrade.sql
-- Desc: 子管理员角色与工作流创建权限初始化脚本
-- =============================================

SET NAMES utf8mb4;

SET @next_role_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role`);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '子管理员', 'SUB_ADMIN', 3, 1, 'SELF', 0,
       0, 0, '2026-07-07 00:00:00', 1, 'system', '2026-07-07 00:00:00',
       1, 'system', 0, '可查看全部权限并创建工作流定义'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_role`
    WHERE `role_code` = 'SUB_ADMIN'
      AND `is_deleted` = 0
);

SET @sub_admin_role_id := (
    SELECT `id`
    FROM `sys_role`
    WHERE `role_code` = 'SUB_ADMIN'
      AND `is_deleted` = 0
    LIMIT 1
);

SET @permission_view_id := (
    SELECT `id`
    FROM `sys_permission`
    WHERE `permission_code` = 'system:permission:view'
      AND `is_deleted` = 0
    LIMIT 1
);

SET @workflow_import_id := (
    SELECT `id`
    FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:import'
      AND `is_deleted` = 0
    LIMIT 1
);

SET @next_role_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_permission`);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1, @sub_admin_role_id, @permission_view_id, 0, 0,
       '2026-07-07 00:00:00', 1, 'system', '2026-07-07 00:00:00', 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL
  AND @permission_view_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission`
    WHERE `role_id` = @sub_admin_role_id
      AND `permission_id` = @permission_view_id
      AND `is_deleted` = 0
);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1, @sub_admin_role_id, @workflow_import_id, 0, 0,
       '2026-07-07 00:00:00', 1, 'system', '2026-07-07 00:00:00', 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL
  AND @workflow_import_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission`
    WHERE `role_id` = @sub_admin_role_id
      AND `permission_id` = @workflow_import_id
      AND `is_deleted` = 0
);
