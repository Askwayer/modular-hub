# CLAUDE.md

This file provides a concise operating summary for Claude Code in this repository. Detailed engineering constraints live in `AICoding/rules/be-vita-ai-codegen-rules.md`.

## 仓库事实

- 技术栈：Spring Boot 3.5.0、JDK 17、Maven、MyBatis-Plus、Sa-Token、Redisson、MySQL。
- 核心模块：`vita-common`、`vita-service`、`vita-workflow`、`vita-admin`、`vita-openapi`、`vita-scheduler`、`vita-generator`、`vita-starters`。
- 依赖主链：`vita-admin -> vita-service -> vita-common`、`vita-admin -> vita-workflow -> vita-service -> vita-common`、`vita-openapi -> vita-service -> vita-common`、`vita-scheduler -> vita-service -> vita-common`。
- `vita-common`、`vita-service` 不承载工作流契约或实现；工作流端口、业务域与实现统一位于 `vita-workflow`。
- `vita-starters` 是 profile 驱动的聚合父模块，当前包含 `captcha`、`file`、`i18n`、`poi` 四个 starter。
- 文档总导航在 `docs/README.md`；需求、架构、开发指南分别位于 `docs/requirements/`、`docs/architecture/`、`docs/development/`。
- 初始化与升级脚本在 `sql/init/`、`sql/upgrade/`；运行日志统一落在根目录 `data/logs/be-vita/`，可通过 `VITA_LOG_HOME` 覆盖。

## 常用命令

- 编译：`mvn clean compile`
- 全仓测试：`mvn test`
- 启动管理端：`mvn -pl vita-admin -am spring-boot:run`
- 启动公开接口：`mvn -pl vita-openapi -am spring-boot:run`
- 启动调度端：`mvn -pl vita-scheduler -am spring-boot:run`
- 测试服务层：`mvn -pl vita-service -am test`
- 代码生成：`mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`
- 测试验证码 starter：`mvn -f vita-starters/pom.xml -Pstarter-captcha test`
- 初始化可选 CodeGraph：`./tools/codegraph/codegraph.sh init`

## 执行摘要

- 默认遵守 `AICoding/rules/be-vita-ai-codegen-rules.md`；本文件不再复制一套独立规范。
- 开始实现或评估前必须检查目标模块目录、技术栈、`git status --short --branch` 和局部约定；涉及历史改动时先查中央文档、提交记录和现有实现。
- 检索必须先精确定位再读取必要片段，长日志、构建错误、SQL、Mapper XML 和差异输出必须限制范围；已固化背景只引用文档路径和章节。
- Controller 只接参与返回，业务编排放 Service，单表持久化细节放 Mapper。
- 新增表如需标准 CRUD，必须先参照 `vita-generator` 模板生成骨架，再做业务改造，不允许直接随意手写一套。
- 核心业务代码要求在关键业务规则、事务边界、状态流转、并发控制和外部集成位置补中文注释，不要求逐行加注释。
- 新建表默认字段规范适用于除纯关联表外的所有新表：`tenant_id`、`create_time`、`update_time`、`is_deleted`、`create_by_id`、`create_by`、`update_by_id`、`update_by`、`version`、`remark`。
- 代表性样例使用 `sys_user`；`pwd_update_date` 是 `sys_user` 业务字段，不属于全局强制字段。
- 任何认证、RBAC、日志、DDL 或初始化脚本变更，都必须同步更新 `docs/` 和 `sql/`。
- CodeGraph 可用时，跨模块调用链、接口实现、影响范围和 MyBatis XML 映射应优先使用精确符号查询；所有仓库操作必须通过 `./tools/codegraph/codegraph.sh`。
- 多级接口、Port/Adapter、多实现类和反射链路必须回退 `rg` 与源码复核；禁止删除或绕过 `tools/codegraph/`。
- CodeGraph 仅辅助代码理解，不能替代 Java 编译、Maven 测试或源码事实；安装与回滚见 `docs/development/codegraph-integration.md`。
