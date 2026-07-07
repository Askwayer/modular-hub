-- =============================================
-- File: 20260707_workflow_category_upgrade.sql
-- Desc: 工作流分类表与默认分类初始化脚本
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `workflow_category`
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

SET @next_workflow_category_id := (SELECT COALESCE(MAX(`id`), 0) FROM `workflow_category`);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1, 0, 'FINANCE', '财务类', NULL, 10,
       1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
       1, 'system', 0, '工作流默认分类'
WHERE NOT EXISTS (
    SELECT 1
    FROM `workflow_category`
    WHERE `category_code` = 'FINANCE'
      AND `is_deleted` = 0
);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1, 0, 'REIMBURSE', '报销审批', 'FINANCE', 11,
       1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
       1, 'system', 0, '报销审批流程分类'
WHERE NOT EXISTS (
    SELECT 1
    FROM `workflow_category`
    WHERE `category_code` = 'REIMBURSE'
      AND `is_deleted` = 0
);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1, 0, 'HR', '人事类', NULL, 20,
       1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
       1, 'system', 0, '工作流默认分类'
WHERE NOT EXISTS (
    SELECT 1
    FROM `workflow_category`
    WHERE `category_code` = 'HR'
      AND `is_deleted` = 0
);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1, 0, 'ADMIN', '行政类', NULL, 30,
       1, '2026-07-07 00:00:00', '2026-07-07 00:00:00', 0, 1, 'system',
       1, 'system', 0, '工作流默认分类'
WHERE NOT EXISTS (
    SELECT 1
    FROM `workflow_category`
    WHERE `category_code` = 'ADMIN'
      AND `is_deleted` = 0
);
