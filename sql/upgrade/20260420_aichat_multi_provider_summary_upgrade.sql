-- =============================================
-- File: 20260420_aichat_multi_provider_summary_upgrade.sql
-- Desc: aichat 多模型与摘要压缩升级脚本
-- =============================================

ALTER TABLE `ai_conversation`
    ADD COLUMN `current_provider_code` VARCHAR(64) DEFAULT NULL COMMENT '当前模型提供者编码' AFTER `current_model_id`;

ALTER TABLE `ai_message`
    ADD COLUMN `provider_code` VARCHAR(64) DEFAULT NULL COMMENT '模型提供者编码' AFTER `provider_request_id`;

ALTER TABLE `ai_conversation_summary`
    ADD COLUMN `provider_code` VARCHAR(64) DEFAULT NULL COMMENT '摘要生成提供者编码' AFTER `version_no`,
    ADD COLUMN `model_code` VARCHAR(64) DEFAULT NULL COMMENT '摘要生成模型编码' AFTER `provider_code`;

ALTER TABLE `ai_chat_request`
    ADD COLUMN `provider_code` VARCHAR(64) DEFAULT NULL COMMENT '模型提供者编码' AFTER `request_id`,
    ADD COLUMN `request_scene` VARCHAR(32) NOT NULL DEFAULT 'CHAT' COMMENT '请求场景 CHAT/ROUND_SUMMARY/SUMMARY_COMPRESS' AFTER `provider_request_id`;

ALTER TABLE `ai_chat_request`
    ADD INDEX `idx_vai_req_scene_status` (`request_scene`, `status`);

UPDATE `ai_conversation`
SET `current_provider_code` = 'qwen'
WHERE `current_provider_code` = 'dashscope';

UPDATE `ai_message`
SET `provider_code` = 'qwen'
WHERE `provider_code` = 'dashscope';

UPDATE `ai_conversation_summary`
SET `provider_code` = 'qwen'
WHERE `provider_code` = 'dashscope';

UPDATE `ai_chat_request`
SET `provider_code` = 'qwen'
WHERE `provider_code` = 'dashscope';
