# CLAUDE.md

This file provides a concise operating summary for Claude Code in this repository. Detailed engineering constraints live in `AICoding/rules/be-vita-ai-codegen-rules.md`.

## 仓库事实

- 技术栈：Spring Boot 3.5.0、JDK 17、Maven、MyBatis-Plus、Sa-Token、Redisson、MySQL、Spring AI。
- 核心模块：`vita-common`、`vita-service`、`vita-admin`、`vita-aichat`、`vita-generator`、`vita-starters`。
- 依赖主链：`vita-admin -> vita-service -> vita-common`，`vita-aichat -> vita-common`。
- `vita-starters` 是 profile 驱动的聚合父模块，当前包含 `ai`、`captcha`、`file`、`i18n`、`poi` 五个 starter。
- 设计文档在 `docs/system/`、`docs/aichat/`、`docs/starter/`；初始化与升级脚本在 `sql/init/`、`sql/upgrade/`；日志目录为 `logs/vita-admin/`、`logs/vita-aichat/`。

## 常用命令

- 编译：`mvn clean compile`
- 全仓测试：`mvn test`
- 启动管理端：`mvn -pl vita-admin -am spring-boot:run`
- 启动 AI Chat：`mvn -pl vita-aichat -am spring-boot:run`
- 测试服务层：`mvn -pl vita-service -am test`
- 代码生成：`mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`
- 测试验证码 starter：`mvn -f vita-starters/pom.xml -Pstarter-captcha test`

## 执行摘要

- 默认遵守 `AICoding/rules/be-vita-ai-codegen-rules.md`；本文件不再复制一套独立规范。
- Controller 只接参与返回，业务编排放 Service，单表持久化细节放 Mapper。
- 新建表默认字段规范适用于除纯关联表外的所有新表：`tenant_id`、`create_time`、`update_time`、`is_deleted`、`create_by_id`、`create_by`、`update_by_id`、`update_by`、`version`、`remark`。
- 代表性样例使用 `sys_user`；`pwd_update_date` 是 `sys_user` 业务字段，不属于全局强制字段。
- 任何认证、RBAC、日志、AI Chat、DDL 或初始化脚本变更，都必须同步更新 `docs/` 和 `sql/`。
