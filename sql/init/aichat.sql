-- =============================================
-- File: ai_init.sql
-- Desc: be-vita AI 对话模块初始化脚本
-- DB  : MySQL 8.x
-- Scope:
--   1) ai_conversation
--   2) ai_message
--   3) ai_conversation_summary
--
-- Usage:
--   直接在目标库执行本脚本即可
--   已使用 CREATE TABLE IF NOT EXISTS,支持重复执行
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. AI 会话表
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id`                       BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`                BIGINT                  DEFAULT NULL COMMENT '租户ID',
    `user_id`                  BIGINT       NOT NULL COMMENT '用户ID',
    `title`                    VARCHAR(200) NOT NULL    DEFAULT '新对话' COMMENT '会话标题',
    `title_source`             VARCHAR(16)  NOT NULL    DEFAULT 'AUTO' COMMENT '标题来源 AUTO/MANUAL',
    `status`                   VARCHAR(16)  NOT NULL    DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/ARCHIVED/DELETED',
    `pinned`                   TINYINT      NOT NULL    DEFAULT 0 COMMENT '是否置顶 0否 1是',
    `last_message_at`          DATETIME                DEFAULT NULL COMMENT '最后消息时间',
    `message_count`            INT          NOT NULL    DEFAULT 0 COMMENT '消息总数',
    `round_count`              INT          NOT NULL    DEFAULT 0 COMMENT '轮次数 1轮=问+答',
    `current_model_id`         BIGINT                  DEFAULT NULL COMMENT '当前模型ID',
    `current_model_code`       VARCHAR(64)             DEFAULT NULL COMMENT '当前模型编码',
    `system_prompt`            VARCHAR(4000)           DEFAULT NULL COMMENT '系统提示词',
    `temperature`              DECIMAL(4, 2)           DEFAULT NULL COMMENT '温度参数',
    `max_tokens`               INT                     DEFAULT NULL COMMENT '单次最大输出token',
    `max_context_messages`     INT          NOT NULL    DEFAULT 12 COMMENT '最大上下文消息数',
    `summary_version`          INT          NOT NULL    DEFAULT 0 COMMENT '当前摘要版本',
    `summary_status`           VARCHAR(16)  NOT NULL    DEFAULT 'NONE' COMMENT '摘要状态 NONE/READY/STALE/GENERATING',
    `ext_json`                 JSON                    DEFAULT NULL COMMENT '扩展字段',
    `is_deleted`               TINYINT(1)              DEFAULT 0 COMMENT '是否删除(0-否,1-是)',
    `create_time`              DATETIME     NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`             BIGINT                  DEFAULT NULL COMMENT '新增用户ID',
    `create_by`                VARCHAR(50)             DEFAULT NULL COMMENT '新增用户名称',
    `update_time`              DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`             BIGINT                  DEFAULT NULL COMMENT '更新用户ID',
    `update_by`                VARCHAR(50)             DEFAULT NULL COMMENT '更新用户名称',
    `version`                  BIGINT                  DEFAULT 0 COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY `idx_vai_conv_user_status_time` (`user_id`, `status`, `last_message_at`),
    KEY `idx_vai_conv_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_vai_conv_user_pinned_time` (`user_id`, `pinned`, `last_message_at`),
    KEY `idx_vai_conv_deleted_time` (`is_deleted`, `update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI会话表';

-- =============================================
-- 2. AI 消息表
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id`                    BIGINT        NOT NULL COMMENT '主键ID',
    `tenant_id`             BIGINT                   DEFAULT NULL COMMENT '租户ID',
    `conversation_id`       BIGINT        NOT NULL COMMENT '会话ID',
    `user_id`               BIGINT        NOT NULL COMMENT '所属用户ID',
    `role_type`             VARCHAR(16)   NOT NULL COMMENT '角色 USER/ASSISTANT/SYSTEM/SUMMARY',
    `message_type`          VARCHAR(16)   NOT NULL    DEFAULT 'TEXT' COMMENT '消息类型 TEXT/IMAGE/FILE/EVENT',
    `reply_to_id`           BIGINT                   DEFAULT NULL COMMENT '回复消息ID,ASSISTANT通常指向对应USER消息',
    `request_id`            VARCHAR(64)              DEFAULT NULL COMMENT '请求ID,前后端链路追踪',
    `model_id`              BIGINT                   DEFAULT NULL COMMENT '模型ID',
    `model_code`            VARCHAR(64)              DEFAULT NULL COMMENT '模型编码',
    `content`               LONGTEXT                 COMMENT '最终展示内容',
    `reasoning_content`     LONGTEXT                 COMMENT '推理内容或思考内容',
    `content_status`        VARCHAR(16)   NOT NULL    DEFAULT 'DONE' COMMENT '内容状态 GENERATING/DONE/ERROR/CANCELLED',
    `use_context`           TINYINT       NOT NULL    DEFAULT 1 COMMENT '是否参与上下文 0否 1是',
    `token_input`           INT                      DEFAULT NULL COMMENT '输入token',
    `token_output`          INT                      DEFAULT NULL COMMENT '输出token',
    `token_total`           INT                      DEFAULT NULL COMMENT '总token',
    `error_code`            VARCHAR(64)              DEFAULT NULL COMMENT '错误码',
    `error_message`         VARCHAR(1000)            DEFAULT NULL COMMENT '错误信息',
    `attachments_json`      JSON                     DEFAULT NULL COMMENT '附件信息 JSON',
    `extra_json`            JSON                     DEFAULT NULL COMMENT '扩展信息 JSON',
    `is_deleted`            TINYINT(1)               DEFAULT 0 COMMENT '是否删除(0-否,1-是)',
    `create_time`           DATETIME      NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`          BIGINT                   DEFAULT NULL COMMENT '新增用户ID',
    `create_by`             VARCHAR(50)              DEFAULT NULL COMMENT '新增用户名称',
    `update_time`           DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`          BIGINT                   DEFAULT NULL COMMENT '更新用户ID',
    `update_by`             VARCHAR(50)              DEFAULT NULL COMMENT '更新用户名称',
    `version`               BIGINT                   DEFAULT 0 COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY `idx_vai_msg_conv_time` (`conversation_id`, `create_time`),
    KEY `idx_vai_msg_conv_role_time` (`conversation_id`, `role_type`, `create_time`),
    KEY `idx_vai_msg_request_id` (`request_id`),
    KEY `idx_vai_msg_reply_to_id` (`reply_to_id`),
    KEY `idx_vai_msg_user_conv` (`user_id`, `conversation_id`),
    KEY `idx_vai_msg_deleted_time` (`is_deleted`, `update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI消息表';

-- =============================================
-- 3. AI 会话摘要表
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_conversation_summary` (
    `id`                        BIGINT        NOT NULL COMMENT '主键ID',
    `tenant_id`                 BIGINT                   DEFAULT NULL COMMENT '租户ID',
    `conversation_id`           BIGINT        NOT NULL COMMENT '会话ID',
    `user_id`                   BIGINT        NOT NULL COMMENT '用户ID',
    `version_no`                INT           NOT NULL COMMENT '摘要版本号',
    `source_message_start_id`   BIGINT                   DEFAULT NULL COMMENT '压缩起始消息ID',
    `source_message_end_id`     BIGINT                   DEFAULT NULL COMMENT '压缩结束消息ID',
    `source_message_count`      INT           NOT NULL    DEFAULT 0 COMMENT '被压缩消息数',
    `summary_text`              LONGTEXT      NOT NULL COMMENT '摘要文本',
    `summary_json`              JSON                     DEFAULT NULL COMMENT '结构化摘要 JSON',
    `token_saved_estimate`      INT                      DEFAULT NULL COMMENT '预估节省token',
    `status`                    VARCHAR(16)   NOT NULL    DEFAULT 'READY' COMMENT '状态 READY/STALE',
    `is_deleted`                TINYINT(1)               DEFAULT 0 COMMENT '是否删除(0-否,1-是)',
    `create_time`               DATETIME      NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`              BIGINT                   DEFAULT NULL COMMENT '新增用户ID',
    `create_by`                 VARCHAR(50)              DEFAULT NULL COMMENT '新增用户名称',
    `update_time`               DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`              BIGINT                   DEFAULT NULL COMMENT '更新用户ID',
    `update_by`                 VARCHAR(50)              DEFAULT NULL COMMENT '更新用户名称',
    `version`                   BIGINT                   DEFAULT 0 COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vai_sum_conv_ver` (`conversation_id`, `version_no`),
    KEY `idx_vai_sum_conv_status` (`conversation_id`, `status`),
    KEY `idx_vai_sum_user_conv` (`user_id`, `conversation_id`),
    KEY `idx_vai_sum_deleted_time` (`is_deleted`, `update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI会话摘要表';

SET FOREIGN_KEY_CHECKS = 1;
