-- =============================================
-- File: 20260703_workflow_import_permission_upgrade.sql
-- Desc: 工作流 JSON 导入权限初始化脚本
-- =============================================

SET NAMES utf8mb4;

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT 2032, '工作流定义导入', 'system:workflow:import', 'MENU_ACTION', NULL, 'POST',
       '/system/workflow/importJson', 'system:workflow:import', 101, 1, 1, 0, 0,
       '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00', 1, 'system',
       0, '导入 Warm-Flow 流程定义 JSON'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:import'
      AND `is_deleted` = 0
);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT 5036, 1, 2032, 0, 0, '2026-07-03 00:00:00',
       1, 'system', '2026-07-03 00:00:00', 1, 'system', 0
WHERE EXISTS (
    SELECT 1
    FROM `sys_permission`
    WHERE `id` = 2032
      AND `is_deleted` = 0
)
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission`
    WHERE `role_id` = 1
      AND `permission_id` = 2032
      AND `is_deleted` = 0
);
