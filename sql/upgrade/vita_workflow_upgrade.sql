-- =============================================
-- File: vita_workflow_upgrade.sql
-- Desc: be-vita 工作流扩展表与 RBAC 数据重建脚本
-- Warning: 本脚本会永久清空自定义工作流分类，仅恢复默认分类。
-- Order: 请先执行 warm_flow_upgrade.sql，再执行本脚本。
-- =============================================

SET NAMES utf8mb4;

-- ----------------------------
-- be-vita workflow extension table
-- ----------------------------
DROP TABLE IF EXISTS `workflow_category`;

CREATE TABLE `workflow_category`
(
    `id`            bigint       NOT NULL COMMENT '分类ID',
    `tenant_id`     bigint                DEFAULT NULL COMMENT '租户ID',
    `category_code` varchar(64)  NOT NULL COMMENT '分类编码',
    `category_name` varchar(100) NOT NULL COMMENT '分类名称',
    `parent_code`   varchar(64)           DEFAULT NULL COMMENT '父分类编码',
    `sort_no`       int          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`   datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`  bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`     varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`  bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`     varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`       bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`        varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_category_code` (`category_code`, `is_deleted`),
    KEY `idx_workflow_category_parent` (`parent_code`),
    KEY `idx_workflow_category_status_sort` (`status`, `sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工作流分类表';

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
VALUES (1, 0, 'FINANCE', '财务类', NULL, 10,
        1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
        1, 'system', 0, '工作流默认分类'),
       (2, 0, 'REIMBURSE', '报销审批', 'FINANCE', 11,
        1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
        1, 'system', 0, '报销审批流程分类'),
       (3, 0, 'HR', '人事类', NULL, 20,
        1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
        1, 'system', 0, '工作流默认分类'),
       (4, 0, 'ADMIN', '行政类', NULL, 30,
        1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
        1, 'system', 0, '工作流默认分类');

-- ----------------------------
-- workflow roles
-- ----------------------------
SET @next_role_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role`);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '财务审批角色', 'FINANCE_APPROVER', 91, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程财务节点'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'FINANCE_APPROVER' AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '综合管理部审批角色', 'GENERAL_AFFAIRS_APPROVER', 92, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程综合管理部节点'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'GENERAL_AFFAIRS_APPROVER' AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '总经理审批角色', 'GENERAL_MANAGER_APPROVER', 93, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程总经理节点'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'GENERAL_MANAGER_APPROVER' AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '子管理员', 'SUB_ADMIN', 3, 1, 'SELF', 0,
       0, 0, '2026-07-07 00:00:00', 1, 'system', '2026-07-07 00:00:00',
       1, 'system', 0, '可查看全部权限并管理工作流'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'SUB_ADMIN' AND `is_deleted` = 0
);

-- ----------------------------
-- workflow permissions
-- ----------------------------
SET @next_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_permission`);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流定义导入', 'system:workflow:import',
       'MENU_ACTION', NULL, 'POST', '/system/workflow/importJson', 'system:workflow:import',
       101, 1, 1, 0, 0, '2026-07-03 00:00:00', 1, 'system',
       '2026-07-03 00:00:00', 1, 'system', 0, '导入 Warm-Flow 流程定义 JSON'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:import' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流定义查看', 'system:workflow:definition:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/definitions/**', 'system:workflow:definition:view',
       102, 1, 1, 0, 0, '2026-07-20 00:00:00', 1, 'system',
       '2026-07-20 00:00:00', 1, 'system', 0, '查看工作流定义列表和详情'
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
       103, 1, 1, 0, 0, '2026-07-20 00:00:00', 1, 'system',
       '2026-07-20 00:00:00', 1, 'system', 0, '发布、取消发布、复制和删除工作流定义'
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
       104, 1, 1, 0, 0, '2026-07-20 00:00:00', 1, 'system',
       '2026-07-20 00:00:00', 1, 'system', 0, '查看工作流实例和流转轨迹'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:instance:view' AND `is_deleted` = 0
);

-- ----------------------------
-- workflow role permissions
-- ----------------------------
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
SET @permission_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:permission:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_import_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:import' AND `is_deleted` = 0
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
SET @next_role_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_permission`);

-- SUPER_ADMIN: workflow import and management permissions.
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_import_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_import_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_import_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_definition_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_definition_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_definition_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_definition_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_definition_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_definition_manage_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_instance_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_instance_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_instance_view_id AND `is_deleted` = 0
  );

-- SUB_ADMIN: permission view, workflow import and management permissions.
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @permission_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @permission_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @permission_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_import_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_import_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_import_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_definition_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_definition_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_definition_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_definition_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_definition_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_definition_manage_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_instance_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_instance_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_instance_view_id AND `is_deleted` = 0
  );
