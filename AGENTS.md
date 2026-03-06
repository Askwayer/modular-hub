# Repository Guidelines

## 项目结构与模块组织
本仓库是 Maven 多模块后端项目（`be-vita`），核心目录如下：
- `vita-admin`：Web 启动模块，入口类 `com.vita.config.VitaAdminApplication`。
- `vita-service`：业务服务层模块（主要承载 domain 服务实现）。
- `vita-common`：公共基础模块（异常、分页、配置、工具类、序列化等）。
- `vita-generator`：MyBatis-Plus 代码生成器与模板（`src/main/resources/templates`）。
- `vita-starters`：Starter 聚合模块，包含 captcha/file/i18n/poi 子 starter。
- `sql/system.sql`：数据库初始化脚本。
- `openspec/`：变更提案、设计、任务与规格文档。

统一使用 `com.vita.*` 包前缀，保持依赖方向清晰：`admin/service -> common`。

## 构建、测试与本地开发命令
- `mvn clean compile`：编译全部模块，并校验依赖关系是否正确。
- `mvn clean package -DskipTests`：快速打包所有模块。
- `mvn test`：运行全仓测试。
- `mvn -pl vita-admin -am spring-boot:run`：在本地运行管理端服务，并自动构建其依赖模块。
- `mvn -pl vita-common -am test`：仅执行公共模块测试。
- `mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`：执行默认代码生成。

开发环境请使用 JDK 17 和 Maven 3.9+。

## 代码风格与命名规范
- Java 使用 4 空格缩进、UTF-8 编码、单文件单 public 类。
- 命名规范：类 `PascalCase`，方法/字段 `camelCase`，常量 `UPPER_SNAKE_CASE`。
- 分层后缀保持一致：`*Controller`、`*Service`、`*ServiceImpl`、`*Mapper`、`*Dto`、`*Vo`、`*Model`。
- Controller 仅做参数接收与返回封装，业务逻辑放 Service，数据访问留在 Mapper。

## 测试规范
- 测试代码放在 `src/test/java` 下，并与生产代码保持相同包路径结构。
- 推荐命名：`*Test` 表示单元测试，`*IT` 表示集成测试。
- 当前 `vita-common` 使用 JUnit 5（`spring-boot-starter-test`）。
- 重点覆盖新增业务逻辑、参数校验、异常分支和加解密等工具类边界条件。

## 提交与 Pull Request 规范
- 遵循仓库历史中已有的 Conventional Commit 风格：`feat: ...`、`fix: ...`、`refactor: ...`（可选 scope，例如 `feat(common): ...`）。
- 每次提交应尽量保持原子性，避免将重构与新功能混在同一个提交中。
- PR 应包含：变更摘要、风险/影响说明、验证步骤，以及关联的 issue 或 spec。
- 若涉及数据库或配置行为变更，请同步更新 `sql/` 脚本或配置说明，并给出迁移步骤。

## 安全与配置建议
- 不要提交任何密钥信息（数据库密码、Redis 凭据、令牌等）。
- 环境相关配置应放在 profile 配置文件或外部环境变量中。
- 日志中应对敏感字段脱敏，禁止输出明文密码、Token、身份证号等。
