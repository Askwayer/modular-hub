-- =============================================
-- File: 20260421_aichat_context_memory_upgrade.sql
-- Desc: Aichat 上下文预算驱动与内部轮次记忆升级
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_round_memory` (
    `id`                         BIGINT        NOT NULL COMMENT '主键ID',
    `tenant_id`                  BIGINT                   DEFAULT NULL COMMENT '租户ID',
    `conversation_id`            BIGINT        NOT NULL COMMENT '会话ID',
    `user_id`                    BIGINT        NOT NULL COMMENT '用户ID',
    `round_no`                   INT           NOT NULL COMMENT '轮次号，从1开始',
    `source_request_id`          VARCHAR(64)              DEFAULT NULL COMMENT '来源业务请求ID',
    `source_user_message_id`     BIGINT                   DEFAULT NULL COMMENT '来源用户消息ID',
    `source_assistant_message_id` BIGINT                  DEFAULT NULL COMMENT '来源助手消息ID',
    `provider_code`              VARCHAR(64)              DEFAULT NULL COMMENT '记忆生成提供者编码',
    `model_code`                 VARCHAR(64)              DEFAULT NULL COMMENT '记忆生成模型编码',
    `memory_text`                LONGTEXT      NOT NULL COMMENT '轮次记忆文本',
    `memory_json`                JSON                     DEFAULT NULL COMMENT '结构化记忆 JSON',
    `prompt_token_estimate`      INT                      DEFAULT NULL COMMENT '注入提示词估算 token',
    `salience_score`             INT                      DEFAULT NULL COMMENT '显著性评分 0-100',
    `carry_forward`              TINYINT(1)    NOT NULL  DEFAULT 0 COMMENT '是否需要延续到后续轮次',
    `status`                     VARCHAR(16)   NOT NULL  DEFAULT 'READY' COMMENT '状态 READY/ROLLED_UP/SKIPPED',
    `is_deleted`                 TINYINT(1)               DEFAULT 0 COMMENT '是否删除(0-否,1-是)',
    `create_time`                DATETIME      NOT NULL  DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`               BIGINT                   DEFAULT NULL COMMENT '新增用户ID',
    `create_by`                  VARCHAR(50)              DEFAULT NULL COMMENT '新增用户名称',
    `update_time`                DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`               BIGINT                   DEFAULT NULL COMMENT '更新用户ID',
    `update_by`                  VARCHAR(50)              DEFAULT NULL COMMENT '更新用户名称',
    `version`                    BIGINT                   DEFAULT 0 COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vai_round_conv_round` (`conversation_id`, `round_no`),
    KEY `idx_vai_round_conv_status` (`conversation_id`, `status`, `round_no`),
    KEY `idx_vai_round_user_conv` (`user_id`, `conversation_id`),
    KEY `idx_vai_round_deleted_time` (`is_deleted`, `update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI轮次记忆表';

ALTER TABLE `ai_conversation_summary`
    ADD COLUMN `source_round_start_no` INT DEFAULT NULL COMMENT '压缩起始轮次号' AFTER `source_message_count`,
    ADD COLUMN `source_round_end_no` INT DEFAULT NULL COMMENT '压缩结束轮次号' AFTER `source_round_start_no`,
    ADD COLUMN `source_round_memory_start_id` BIGINT DEFAULT NULL COMMENT '压缩起始轮次记忆ID' AFTER `source_round_end_no`,
    ADD COLUMN `source_round_memory_end_id` BIGINT DEFAULT NULL COMMENT '压缩结束轮次记忆ID' AFTER `source_round_memory_start_id`,
    ADD COLUMN `prompt_token_estimate` INT DEFAULT NULL COMMENT '注入摘要时的估算token' AFTER `token_saved_estimate`;

DELETE FROM `ai_message`
 WHERE `role_type` = 'SUMMARY';
