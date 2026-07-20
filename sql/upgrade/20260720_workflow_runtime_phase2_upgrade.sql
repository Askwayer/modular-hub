-- =============================================
-- File: 20260720_workflow_runtime_phase2_upgrade.sql
-- Desc: 工作流二阶段管理权限升级
-- =============================================

SET NAMES utf8mb4;

SET @next_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_permission`);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流定义查看', 'system:workflow:definition:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/definitions/**', 'system:workflow:definition:view',
       102, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '查看工作流定义列表和详情'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:view' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流定义管理', 'system:workflow:definition:manage',
       'MENU_ACTION', NULL, NULL, '/system/workflow/definitions/**', 'system:workflow:definition:manage',
       103, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '发布、取消发布、复制和删除工作流定义'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:manage' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流实例查看', 'system:workflow:instance:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/instances/**', 'system:workflow:instance:view',
       104, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '查看工作流实例和流转轨迹'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:instance:view' AND `is_deleted` = 0
);

SET @next_role_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_permission`);
SET @super_admin_role_id := (
    SELECT `id` FROM `sys_role`
    WHERE `role_code` = 'SUPER_ADMIN' AND `is_deleted` = 0
    LIMIT 1
);
SET @sub_admin_role_id := (
    SELECT `id` FROM `sys_role`
    WHERE `role_code` = 'SUB_ADMIN' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_definition_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_definition_manage_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:manage' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_instance_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:instance:view' AND `is_deleted` = 0
    LIMIT 1
);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_definition_view_id, 0, 0, NOW(),
       1, 'system', NOW(), 1, 'system', 0
FROM DUAL
WHERE @super_admin_role_id IS NOT NULL
  AND @workflow_definition_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id
        AND `permission_id` = @workflow_definition_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_definition_manage_id, 0, 0, NOW(),
       1, 'system', NOW(), 1, 'system', 0
FROM DUAL
WHERE @super_admin_role_id IS NOT NULL
  AND @workflow_definition_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id
        AND `permission_id` = @workflow_definition_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_instance_view_id, 0, 0, NOW(),
       1, 'system', NOW(), 1, 'system', 0
FROM DUAL
WHERE @super_admin_role_id IS NOT NULL
  AND @workflow_instance_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id
        AND `permission_id` = @workflow_instance_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_definition_view_id, 0, 0, NOW(),
       1, 'system', NOW(), 1, 'system', 0
FROM DUAL
WHERE @sub_admin_role_id IS NOT NULL
  AND @workflow_definition_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id
        AND `permission_id` = @workflow_definition_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_definition_manage_id, 0, 0, NOW(),
       1, 'system', NOW(), 1, 'system', 0
FROM DUAL
WHERE @sub_admin_role_id IS NOT NULL
  AND @workflow_definition_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id
        AND `permission_id` = @workflow_definition_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_instance_view_id, 0, 0, NOW(),
       1, 'system', NOW(), 1, 'system', 0
FROM DUAL
WHERE @sub_admin_role_id IS NOT NULL
  AND @workflow_instance_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id
        AND `permission_id` = @workflow_instance_view_id
        AND `is_deleted` = 0
  );
