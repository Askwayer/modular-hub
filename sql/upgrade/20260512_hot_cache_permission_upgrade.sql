-- =============================================
-- File: 20260512_hot_cache_permission_upgrade.sql
-- Desc: 热搜缓存后台权限初始化脚本
-- =============================================

SET NAMES utf8mb4;

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT 2030, '热搜缓存查看', 'system:hot-cache:view', 'MENU_ACTION', NULL, 'GET',
       '/system/hot/cache/status', 'system:hot-cache:view', 91, 1, 1, 0, 0,
       '2026-05-12 00:00:00', 1, 'system', '2026-05-12 00:00:00', 1, 'system',
       0, '涵盖热搜缓存状态查询'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_permission`
    WHERE `permission_code` = 'system:hot-cache:view'
      AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT 2031, '热搜缓存刷新', 'system:hot-cache:refresh', 'MENU_ACTION', NULL, 'POST',
       '/system/hot/cache/refresh,/system/hot/cache/refreshAll', 'system:hot-cache:refresh', 92, 1, 1, 0, 0,
       '2026-05-12 00:00:00', 1, 'system', '2026-05-12 00:00:00', 1, 'system',
       0, '涵盖单源刷新与全量刷新'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_permission`
    WHERE `permission_code` = 'system:hot-cache:refresh'
      AND `is_deleted` = 0
);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT 5033, 1, 2030, 0, 0, '2026-05-12 00:00:00',
       1, 'system', '2026-05-12 00:00:00', 1, 'system', 0
WHERE EXISTS (
    SELECT 1
    FROM `sys_permission`
    WHERE `id` = 2030
      AND `is_deleted` = 0
)
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission`
    WHERE `role_id` = 1
      AND `permission_id` = 2030
      AND `is_deleted` = 0
);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT 5034, 1, 2031, 0, 0, '2026-05-12 00:00:00',
       1, 'system', '2026-05-12 00:00:00', 1, 'system', 0
WHERE EXISTS (
    SELECT 1
    FROM `sys_permission`
    WHERE `id` = 2031
      AND `is_deleted` = 0
)
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission`
    WHERE `role_id` = 1
      AND `permission_id` = 2031
      AND `is_deleted` = 0
);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT 5035, 2, 2030, 0, 0, '2026-05-12 00:00:00',
       1, 'system', '2026-05-12 00:00:00', 1, 'system', 0
WHERE EXISTS (
    SELECT 1
    FROM `sys_permission`
    WHERE `id` = 2030
      AND `is_deleted` = 0
)
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission`
    WHERE `role_id` = 2
      AND `permission_id` = 2030
      AND `is_deleted` = 0
);
