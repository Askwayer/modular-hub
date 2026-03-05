# Repository Guidelines

## 项目结构与模块组织
本仓库是一个 Maven 多模块后端项目（`be-vita`），当前包含：
- `vita-admin`：Spring Boot 启动模块（`com.vita.config.VitaAdminApplication`），配置文件位于 `vita-admin/src/main/resources`。
- `vita-common`：公共核心代码模块（基础实体、异常、分页、序列化器、配置等）。
- `vita-generator`：MyBatis-Plus 代码生成器与 Velocity 模板，模板位于 `vita-generator/src/main/resources/templates`。
- `openspec/`：用于规格驱动开发的变更提案、设计文档和任务跟踪。

统一使用 `com.vita.*` 作为包路径根命名空间，并保持模块依赖单向流动（业务模块 -> common）。

## 构建、测试与本地开发命令
- `mvn clean compile`：编译全部模块，并校验依赖关系是否正确。
- `mvn clean package -DskipTests`：快速打包生成可分发 JAR。
- `mvn test`：运行单元测试（当前测试较少或暂无；后续新增测试时请确保该命令始终可通过）。
- `mvn -pl vita-admin -am spring-boot:run`：在本地运行管理端服务，并自动构建其依赖模块。
- `mvn -pl vita-common -am install`：仅构建并安装 `vita-common` 及其所需上游模块。

开发环境请使用 JDK 17 和 Maven 3.9+。

## 代码风格与命名规范
- Java 代码统一使用 4 个空格缩进、UTF-8 编码、每个文件仅保留一个 public 类。
- 命名规则：类名使用 `PascalCase`，方法和字段使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE`。
- 分层命名后缀需明确：`*Controller`、`*Service`、`*Mapper`、`*Dto`、`*Vo`。
- Controller 保持轻量，业务逻辑放在 Service 层，Mapper 仅负责持久化访问。

## 测试规范
- 测试代码放在 `src/test/java` 下，并与生产代码保持相同包路径结构。
- 推荐命名：`*Test` 表示单元测试，`*IT` 表示集成测试。
- 重点覆盖新增业务逻辑、参数校验和异常分支。
- 当前未配置强制覆盖率门禁，但非简单改动必须补充有意义的测试。

## 提交与 Pull Request 规范
- 遵循仓库历史中已有的 Conventional Commit 风格：`feat: ...`、`fix: ...`、`refactor: ...`（可选 scope，例如 `feat(common): ...`）。
- 每次提交应尽量保持原子性，避免将重构与新功能混在同一个提交中。
- PR 应包含：变更摘要、风险/影响说明、验证步骤，以及关联的 issue 或 spec。
- 若涉及数据库或配置行为变更，请补充 SQL / 配置说明以及迁移步骤。

## 安全与配置建议
- 不要提交任何密钥信息（数据库密码、Redis 凭据、令牌等）。
- 环境相关配置应放在 profile 配置文件或外部环境变量中。
- 日志中应对敏感字段进行脱敏处理。