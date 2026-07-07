-- =============================================
-- File: 20260703_reimburse_workflow_roles_upgrade.sql
-- Desc: 报销审批流程角色初始化脚本
-- =============================================

SET NAMES utf8mb4;

SET @next_role_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role`);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '财务审批角色', 'FINANCE_APPROVER', 91, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程财务节点'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_role`
    WHERE `role_code` = 'FINANCE_APPROVER'
      AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '综合管理部审批角色', 'GENERAL_AFFAIRS_APPROVER', 92, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程综合管理部节点'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_role`
    WHERE `role_code` = 'GENERAL_AFFAIRS_APPROVER'
      AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '总经理审批角色', 'GENERAL_MANAGER_APPROVER', 93, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程总经理节点'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_role`
    WHERE `role_code` = 'GENERAL_MANAGER_APPROVER'
      AND `is_deleted` = 0
);
