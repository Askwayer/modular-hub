# Repository Guidelines

## 项目结构与模块组织
本仓库是 Maven 多模块后端项目（`be-vita`）。`vita-admin` 是管理端启动模块，入口类为 `com.vita.config.VitaAdminApplication`，上下文路径为 `/admin/api`；`vita-aichat` 是 AI Chat 启动模块，入口类为 `com.vita.config.VitaAIChatApplication`，上下文路径为 `/aichat/api`；`vita-service` 承载系统业务服务层；`vita-common` 放公共基础能力（异常、分页、MyBatis-Plus、Redisson、Sa-Token 等）；`vita-generator` 维护代码生成器与模板；`vita-starters` 聚合 `captcha`、`file`、`i18n`、`poi` 四个 starter。

补充资产位于 `sql/init/*.sql`、`sql/upgrade/*.sql`、`docs/system/*`、`docs/aichat/*`，运行日志输出到 `logs/vita-admin/` 和 `logs/vita-aichat/`。

统一使用 `com.vita.*` 包前缀。当前依赖方向以 `pom.xml` 为准，主链路保持：

- `vita-admin -> vita-service -> vita-common`
- `vita-aichat -> vita-common`

业务模块不要直接跨层访问别的模块内部实现，也不要让 `vita-aichat` 依赖 `vita-admin` 的内部代码。

## 构建、测试与本地开发命令
- `mvn clean compile`：编译全部模块，并校验依赖关系是否正确。
- `mvn clean package -DskipTests`：快速打包所有模块。
- `mvn test`：运行全仓测试。
- `mvn -pl vita-admin -am spring-boot:run`：在本地运行管理端服务，并自动构建其依赖模块。
- `mvn -pl vita-aichat -am spring-boot:run`：在本地运行 AI Chat 服务，并自动构建其依赖模块。
- `mvn -pl vita-common -am test`：执行公共模块测试。
- `mvn -pl vita-service -am test`：执行业务服务层测试。
- `mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`：执行默认代码生成。
- `mvn -pl vita-starters/vita-captcha-spring-boot-starter test`：验证 starter 自动装配与验证码服务测试。

开发环境请使用 JDK 17 和 Maven 3.9+。

## 代码风格与命名规范
- Java 使用 4 空格缩进、UTF-8 编码、单文件单 public 类。
- 命名规范：类 `PascalCase`，方法/字段 `camelCase`，常量 `UPPER_SNAKE_CASE`。
- 分层后缀保持一致：`*Controller`、`*Service`、`*ServiceImpl`、`*Mapper`、`*Dto`、`*Vo`、`*Model`。
- Controller 仅做参数接收与返回封装，业务逻辑放 Service，数据访问留在 Mapper。
- AI Chat 当前已存在生成式 CRUD 骨架，正式任务接口和模型接入方案应先更新 `docs/aichat/ai-chat-design.md`，再落实现。
- 设计说明或跨模块方案落到 `docs/system/` 或 `docs/aichat/`，不要只留在提交描述里。

## 测试规范
- 测试代码放在 `src/test/java` 下，并与生产代码保持相同包路径结构。
- 推荐命名：`*Test` 表示单元测试，`*IT` 表示集成测试。
- 重点覆盖新增业务逻辑、参数校验、异常分支、starter 自动装配，以及 AI Chat、认证、RBAC 等关键链路的行为边界。
- 涉及模型网关、流式响应、消息状态流转时，优先补 service 层和状态转换测试。

## 提交与 Pull Request 规范
- 遵循仓库历史中已有的 Conventional Commit 风格：`feat: ...`、`fix: ...`、`refactor: ...`（可选 scope，例如 `feat(aichat): ...`）。
- 每次提交应尽量保持原子性，避免将重构与新功能混在同一个提交中。
- PR 应包含：变更摘要、风险/影响说明、验证步骤，以及关联的 issue 或 spec。
- 若涉及数据库、认证、日志或 AI Chat 方案变更，请同步更新 `sql/`、`docs/system/`、`docs/aichat/` 或相关配置说明，并给出迁移步骤。

## 安全与配置建议
- 不要提交任何密钥信息（数据库密码、Redis 凭据、令牌、模型 API Key 等）。
- 环境相关配置应放在 profile 配置文件或外部环境变量中。
- 日志中应对敏感字段脱敏，禁止输出明文密码、Token、身份证号、模型 API Key 等；本地排查问题时优先查看 `logs/vita-admin/` 或 `logs/vita-aichat/`。
