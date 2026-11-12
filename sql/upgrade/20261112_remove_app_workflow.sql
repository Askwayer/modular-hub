-- =============================================
-- File: 20261112_remove_app_workflow.sql
-- Desc: 基础框架裁剪，清理 App 账号与工作流遗留数据
-- Risk: 永久删除 App 账号、OAuth 绑定、报销和流程历史；执行前必须完成备份
-- Note: 仅用于已部署旧版本的环境，新建环境不需要执行；停止旧应用后人工执行
-- Note: 现有管理端权限缓存需失效或要求重新登录；仅清理旧 App 专属 Redis 命名空间
-- =============================================

SET NAMES utf8mb4;

-- 先清理授权关系，再删除权限定义，保留其余系统角色和权限。
DELETE FROM `sys_role_permission`
WHERE `permission_id` IN (
    SELECT `id`
    FROM `sys_permission`
    WHERE `permission_code` LIKE 'system:workflow:%'
);

DELETE FROM `sys_permission`
WHERE `permission_code` LIKE 'system:workflow:%';

-- 仓库没有内置工作流菜单；部署环境自行创建的菜单需按实际归属单独清理。
-- 先删除业务与扩展数据，再删除引擎表；明确列出表名，避免误删其他业务表。
DROP TABLE IF EXISTS `workflow_reimbursement`;
DROP TABLE IF EXISTS `workflow_task_copy`;
DROP TABLE IF EXISTS `workflow_instance_metadata`;
DROP TABLE IF EXISTS `workflow_assignee_rule`;
DROP TABLE IF EXISTS `workflow_category`;
DROP TABLE IF EXISTS `flow_user`;
DROP TABLE IF EXISTS `flow_his_task`;
DROP TABLE IF EXISTS `flow_task`;
DROP TABLE IF EXISTS `flow_instance`;
DROP TABLE IF EXISTS `flow_skip`;
DROP TABLE IF EXISTS `flow_node`;
DROP TABLE IF EXISTS `flow_form`;
DROP TABLE IF EXISTS `flow_definition`;
DROP TABLE IF EXISTS `app_oauth_account`;
DROP TABLE IF EXISTS `app_user`;
