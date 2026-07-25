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
