-- =============================================
-- File: market_upgrade.sql
-- Desc: A股行情数据大屏持久化表幂等升级脚本
-- Warning: 本脚本仅补充缺失表，不删除或覆盖已有行情业务数据。
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `market_stock`
(
    `id`                 bigint       NOT NULL COMMENT '股票主键ID',
    `tenant_id`          bigint                DEFAULT NULL COMMENT '租户ID',
    `stock_code`         varchar(16)  NOT NULL COMMENT '股票编号',
    `stock_name`         varchar(100) NOT NULL COMMENT '股票名称',
    `market`             varchar(8)   NOT NULL COMMENT '所属市场 SH/SZ/BJ',
    `listing_status`     varchar(20)  NOT NULL DEFAULT 'LISTED' COMMENT '上市状态 LISTED/SUSPENDED/DELISTED',
    `monitor_enabled`    tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否启用资金监控 1是 0否',
    `enabled_time`       datetime              DEFAULT NULL COMMENT '最近一次启用监控时间',
    `display_order`      int          NOT NULL DEFAULT '0' COMMENT '监控显示顺序',
    `source_update_time` datetime              DEFAULT NULL COMMENT '数据源更新时间',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`        datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`       bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`          varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`       bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`          varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`            bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`             varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_market_stock_code` (`stock_code`),
    KEY `idx_market_stock_monitor` (`monitor_enabled`, `listing_status`, `display_order`, `enabled_time`),
    KEY `idx_market_stock_market_status` (`market`, `listing_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='A股股票字典';

CREATE TABLE IF NOT EXISTS `market_stock_fund_point`
(
    `id`              bigint         NOT NULL COMMENT '资金采样点ID',
    `tenant_id`       bigint                  DEFAULT NULL COMMENT '租户ID',
    `stock_code`      varchar(16)    NOT NULL COMMENT '股票编号',
    `trade_date`      date           NOT NULL COMMENT '交易日',
    `sample_bucket`   datetime       NOT NULL COMMENT '40秒采样桶起始时间',
    `sample_time`     datetime       NOT NULL COMMENT '数据源实际采样时间',
    `inflow_amount`   decimal(24, 2) NOT NULL COMMENT '资金流入额，单位人民币元',
    `outflow_amount`  decimal(24, 2) NOT NULL COMMENT '资金流出额，单位人民币元且保持正数',
    `net_amount`      decimal(24, 2) NOT NULL COMMENT '资金净额，单位人民币元',
    `latest_price`    decimal(20, 4) NOT NULL COMMENT '最新价',
    `change_percent`  decimal(12, 6) NOT NULL COMMENT '涨跌幅，单位百分比',
    `turnover_amount` decimal(24, 2) NOT NULL COMMENT '成交额，单位人民币元',
    `turnover_rate`   decimal(12, 6) NOT NULL COMMENT '换手率，单位百分比',
    `create_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`     datetime                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)              DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`    bigint                  DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50)             DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`    bigint                  DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50)             DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                  DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500)            DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_market_fund_stock_bucket` (`stock_code`, `sample_bucket`),
    KEY `idx_market_fund_trade_stock` (`trade_date`, `stock_code`, `sample_bucket`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='A股个股资金40秒采样点';

CREATE TABLE IF NOT EXISTS `market_daily_summary`
(
    `id`                 bigint         NOT NULL COMMENT '市场日汇总ID',
    `tenant_id`          bigint                  DEFAULT NULL COMMENT '租户ID',
    `trade_date`         date           NOT NULL COMMENT '交易日',
    `sh_turnover_amount` decimal(24, 2)          DEFAULT NULL COMMENT '沪市成交额，单位人民币元',
    `sz_turnover_amount` decimal(24, 2)          DEFAULT NULL COMMENT '深市成交额，单位人民币元',
    `bj_turnover_amount` decimal(24, 2)          DEFAULT NULL COMMENT '北交所成交额，单位人民币元',
    `up_count`           int                     DEFAULT NULL COMMENT '上涨家数',
    `down_count`         int                     DEFAULT NULL COMMENT '下跌家数',
    `flat_count`         int                     DEFAULT NULL COMMENT '平盘家数',
    `limit_up_count`     int                     DEFAULT NULL COMMENT '涨停家数',
    `limit_down_count`   int                     DEFAULT NULL COMMENT '跌停家数',
    `main_net_inflow`    decimal(24, 2)          DEFAULT NULL COMMENT '主力净流入，单位人民币元',
    `last_valid_time`    datetime       NOT NULL COMMENT '最后有效行情时间',
    `is_finalized`       tinyint(1)     NOT NULL DEFAULT '0' COMMENT '是否收盘定稿 1是 0否',
    `create_time`        datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`        datetime                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         tinyint(1)              DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`       bigint                  DEFAULT NULL COMMENT '新增用户ID',
    `create_by`          varchar(50)             DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`       bigint                  DEFAULT NULL COMMENT '更新用户ID',
    `update_by`          varchar(50)             DEFAULT NULL COMMENT '更新用户名称',
    `version`            bigint                  DEFAULT '0' COMMENT '乐观锁',
    `remark`             varchar(500)            DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_market_daily_trade_date` (`trade_date`),
    KEY `idx_market_daily_finalized` (`is_finalized`, `trade_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='A股市场日汇总';

-- 行情菜单按 route_name 幂等补齐。
SET @next_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_menu`);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`, `component_path`,
                        `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`, `status`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_menu_id, 0, '行情大屏', 'CONTENTS', 'Market', '/market', 'MarketLayout', 'trend-charts',
       20, 1, 0, 1, 0, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, 'A股行情数据大屏目录'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `route_name` = 'Market' AND `is_deleted` = 0);

SET @market_root_menu_id = (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'Market' AND `is_deleted` = 0 LIMIT 1);
UPDATE `sys_menu`
SET `component_path` = 'MarketLayout',
    `update_time` = NOW(),
    `update_by_id` = 1,
    `update_by` = 'system'
WHERE `id` = @market_root_menu_id
  AND (`component_path` IS NULL OR `component_path` <> 'MarketLayout');

SET @next_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_menu`);
INSERT INTO `˚` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`, `component_path`,
                        `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`, `status`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_menu_id, @market_root_menu_id, '大盘与板块总览', 'MENU', 'MarketOverview', '/market/overview',
       'market/overview/index', 'data-line', 21, 1, 0, 0, 0, 1, 1, 0, 0,
       NOW(), 1, 'system', NOW(), 1, 'system', 0, '指数、市场和板块行情总览'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `route_name` = 'MarketOverview' AND `is_deleted` = 0);

SET @next_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_menu`);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`, `component_path`,
                        `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`, `status`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_menu_id, @market_root_menu_id, '个股资金监控', 'MENU', 'MarketStockMonitor', '/market/stockMonitor',
       'market/stockMonitor/index', 'money', 22, 1, 0, 0, 0, 1, 1, 0, 0,
       NOW(), 1, 'system', NOW(), 1, 'system', 0, '启用股票资金监控'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `route_name` = 'MarketStockMonitor' AND `is_deleted` = 0);

SET @market_overview_menu_id =
    (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'MarketOverview' AND `is_deleted` = 0 LIMIT 1);
SET @market_monitor_menu_id =
    (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'MarketStockMonitor' AND `is_deleted` = 0 LIMIT 1);

-- 权限按 permission_code 幂等补齐。
SET @next_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_permission`);
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id, '行情大屏查看', 'market:dashboard:view', 'MENU_ACTION', @market_overview_menu_id, NULL,
       '/market/**', 'market:dashboard:view', 201, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0,
       '查看行情大屏并触发受限频刷新'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `permission_code` = 'market:dashboard:view' AND `is_deleted` = 0
);

SET @next_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_permission`);
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id, '监控股票管理', 'market:stock:manage', 'MENU_ACTION', @market_monitor_menu_id, 'POST',
       '/market/stock/enable,/market/stock/disable,/market/stock/reorder', 'market:stock:manage',
       202, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '启用、停用和排序监控股票'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `permission_code` = 'market:stock:manage' AND `is_deleted` = 0
);

SET @next_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_permission`);
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id, '股票字典同步', 'market:stock:sync', 'MENU_ACTION', @market_monitor_menu_id, 'POST',
       '/market/stock/dictionary/sync', 'market:stock:sync',
       203, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '手动同步沪深京股票字典'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `permission_code` = 'market:stock:sync' AND `is_deleted` = 0
);

SET @market_view_permission_id =
    (SELECT `id` FROM `sys_permission` WHERE `permission_code` = 'market:dashboard:view' AND `is_deleted` = 0 LIMIT 1);
SET @market_manage_permission_id =
    (SELECT `id` FROM `sys_permission` WHERE `permission_code` = 'market:stock:manage' AND `is_deleted` = 0 LIMIT 1);
SET @market_sync_permission_id =
    (SELECT `id` FROM `sys_permission` WHERE `permission_code` = 'market:stock:sync' AND `is_deleted` = 0 LIMIT 1);

-- 三类角色均可读取行情，只有超级管理员和子管理员拥有写权限。
SET @super_admin_role_id =
    (SELECT `id` FROM `sys_role` WHERE `role_code` = 'SUPER_ADMIN' AND `is_deleted` = 0 LIMIT 1);
SET @common_user_role_id =
    (SELECT `id` FROM `sys_role` WHERE `role_code` = 'COMMON_USER' AND `is_deleted` = 0 LIMIT 1);
SET @sub_admin_role_id =
    (SELECT `id` FROM `sys_role` WHERE `role_code` = 'SUB_ADMIN' AND `is_deleted` = 0 LIMIT 1);

SET @market_root_menu_id = (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'Market' AND `is_deleted` = 0 LIMIT 1);
SET @market_overview_menu_id =
    (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'MarketOverview' AND `is_deleted` = 0 LIMIT 1);
SET @market_monitor_menu_id =
    (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'MarketStockMonitor' AND `is_deleted` = 0 LIMIT 1);

-- 菜单树必须显式授予根目录和两个子菜单。
SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @super_admin_role_id, @market_root_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @super_admin_role_id AND `menu_id` = @market_root_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @super_admin_role_id, @market_overview_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @super_admin_role_id AND `menu_id` = @market_overview_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @super_admin_role_id, @market_monitor_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @super_admin_role_id AND `menu_id` = @market_monitor_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @common_user_role_id, @market_root_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @common_user_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @common_user_role_id AND `menu_id` = @market_root_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @common_user_role_id, @market_overview_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @common_user_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @common_user_role_id AND `menu_id` = @market_overview_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @common_user_role_id, @market_monitor_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @common_user_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @common_user_role_id AND `menu_id` = @market_monitor_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @sub_admin_role_id, @market_root_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @sub_admin_role_id AND `menu_id` = @market_root_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @sub_admin_role_id, @market_overview_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @sub_admin_role_id AND `menu_id` = @market_overview_menu_id AND `is_deleted` = 0
);

SET @next_role_menu_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id, @sub_admin_role_id, @market_monitor_menu_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = @sub_admin_role_id AND `menu_id` = @market_monitor_menu_id AND `is_deleted` = 0
);

SET @next_role_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id, @super_admin_role_id, @market_view_permission_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_permission`
    WHERE `role_id` = @super_admin_role_id AND `permission_id` = @market_view_permission_id AND `is_deleted` = 0
);

SET @next_role_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id, @common_user_role_id, @market_view_permission_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @common_user_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_permission`
    WHERE `role_id` = @common_user_role_id AND `permission_id` = @market_view_permission_id AND `is_deleted` = 0
);

SET @next_role_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id, @sub_admin_role_id, @market_view_permission_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_permission`
    WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @market_view_permission_id AND `is_deleted` = 0
);

SET @next_role_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id, @super_admin_role_id, @market_manage_permission_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_permission`
    WHERE `role_id` = @super_admin_role_id AND `permission_id` = @market_manage_permission_id AND `is_deleted` = 0
);

SET @next_role_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id, @super_admin_role_id, @market_sync_permission_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_permission`
    WHERE `role_id` = @super_admin_role_id AND `permission_id` = @market_sync_permission_id AND `is_deleted` = 0
);

SET @next_role_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id, @sub_admin_role_id, @market_manage_permission_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_permission`
    WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @market_manage_permission_id AND `is_deleted` = 0
);

SET @next_role_permission_id = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id, @sub_admin_role_id, @market_sync_permission_id,
       0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `sys_role_permission`
    WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @market_sync_permission_id AND `is_deleted` = 0
);
