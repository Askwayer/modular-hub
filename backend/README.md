# be-vita

`be-vita` 是基于 Spring Boot 3.5.0、JDK 17、Maven 多模块结构的后端仓库，当前维护管理端、调度端、公共基础能力、平台服务层、部门业务模块、代码生成器和多个 Spring Boot starter。

## 文档分工

- `README.md`：仓库总览、模块结构、启动入口与常用命令。
- `AGENTS.md`：面向 AI 任务的规划、Skill 路由与执行控制入口。
- `AICoding/rules/be-vita-ai-codegen-rules.md`：唯一详细且最高优先级的 AI 代码生成规则源。
- `AICoding/skills/`：可复制的 `common-*` 通用基座及注入本仓库约束的 `be-vita-*` 适配层。

## 当前模块结构

```text
be-vita
├── vita-common
├── vita-service
├── vita-business
│   ├── vita-ah-waste
│   ├── vita-gm-purchase
│   ├── vita-gm-mailrec
│   └── vita-cw-aiaudit
├── vita-generator
├── vita-starters
│   ├── vita-captcha-spring-boot-starter
│   ├── vita-file-spring-boot-starter
│   ├── vita-i18n-spring-boot-starter
│   └── vita-poi-spring-boot-starter
├── vita-admin
├── vita-scheduler
├── AICoding
│   ├── rules
│   └── skills
├── tools
│   └── codegraph
├── sql
│   ├── init
│   └── upgrade
└── docs
    ├── requirements
    ├── architecture
    └── development
```

## 模块职责与依赖

- `vita-common`：公共基础模块，承载统一返回、异常、分页、MyBatis-Plus、Redisson、Sa-Token、工具类和基础模型。
- `vita-service`：平台服务层，承载管理认证、RBAC、系统管理等平台能力。
- `vita-business`：部门业务聚合父模块，使用 `pom` 打包，仅负责组织子模块；各部门业务实现位于各自子模块。
- `vita-admin`：管理端启动模块，入口类 `com.vita.config.VitaAdminApplication`，上下文路径 `/admin/api`。
- `vita-scheduler`：调度任务启动模块，入口类 `com.vita.config.VitaSchedulerApplication`，上下文路径 `/scheduler/api`。
- `vita-generator`：代码生成器与模板模块，入口 `com.vita.config.CodeGenerator`。
- `vita-starters`：starter 聚合父模块，聚合验证码、文件、国际化与导入导出组件，由应用按需依赖。

主依赖链以根 `pom.xml` 为准：

```text
vita-admin   -> vita-service -> vita-common
vita-scheduler -> vita-service -> vita-common
vita-ah-waste -> vita-common
vita-gm-purchase -> vita-common
vita-gm-mailrec -> vita-common
vita-cw-aiaudit -> vita-common
```

## 部门业务模块

| Maven 模块 | Java 根包 |
| --- | --- |
| `vita-business/vita-ah-waste` | `com.vita.business.ah.waste` |
| `vita-business/vita-gm-purchase` | `com.vita.business.gm.purchase` |
| `vita-business/vita-gm-mailrec` | `com.vita.business.gm.mailrec` |
| `vita-business/vita-cw-aiaudit` | `com.vita.business.cw.aiaudit` |

- 四个子模块各有独立 Spring Boot 启动入口，打包生成可执行 JAR；当前仅提供模块名称接口，尚未添加业务表。
- 子模块直接依赖 `vita-common`，后续按需添加文件、国际化、导入导出等 starter；聚合父模块不统一注入运行时依赖。
- 子模块之间不直接依赖，平台服务层也不反向依赖部门业务；业务代码按归属维护在各自模块中。
- 每个应用仅扫描自身业务包，通过独立进程运行。当前名称接口允许匿名访问，按约定直接返回 `text/plain` 字符串；后续业务接口需接入统一身份与数据权限，并沿用 `CommonResult<T>`。
- 名称接口不访问外部服务，应用配置排除了数据源、Redisson 和 Redis 会话存储的自动装配；接入实际业务时再配置持久化、缓存和统一认证。公共基础依赖仍保留。
- 新增部门业务时，在 `vita-business/pom.xml` 注册子模块，在根 `pom.xml` 的 `dependencyManagement` 统一版本，并沿用 `com.vita.business.<部门>.<业务>` 包结构。

### 打包与接口验证

在后端仓库根目录执行 `mvn clean package`，或执行 `mvn -pl :vita-ah-waste -am clean package` 单独构建一个业务及其依赖。

| 模块 | 默认端口 | GET 接口 | 响应正文 |
| --- | --- | --- | --- |
| `vita-ah-waste` | 18081 | `/module/name` | `vita-ah-waste` |
| `vita-gm-purchase` | 18082 | `/module/name` | `vita-gm-purchase` |
| `vita-gm-mailrec` | 18083 | `/module/name` | `vita-gm-mailrec` |
| `vita-cw-aiaudit` | 18084 | `/module/name` | `vita-cw-aiaudit` |

以 `vita-ah-waste` 为例：

```bash
java -jar vita-business/vita-ah-waste/target/vita-ah-waste-1.0.0-SNAPSHOT.jar
curl http://localhost:18081/module/name
```

其他模块使用各自 `target/` 下的同名 JAR。端口可通过 `SERVER_PORT` 环境变量或 `--server.port=端口` 参数覆盖。

## 文档与脚本入口

- 文档总导航：`docs/README.md`
- 集团框架分批验收：`docs/requirements/group-foundation.md`
- 公司、多任职与授权设计：`docs/architecture/system/group/group-foundation-design.md`
- 需求与验收：`docs/requirements/`
- 系统与 Starter 设计：`docs/architecture/`
- 开发与运维指南：`docs/development/`
- AI Skill 导航：`AICoding/skills/README.md`
- 本地开发脚本：`tools/`；CodeGraph 脚本只能位于 `tools/codegraph/`。
- 系统初始化：`sql/init/system.sql`
- 初始化数据：`sql/init/insert.sql`
- 升级脚本：`sql/upgrade/`
- 运行日志：`data/logs/be-vita/`；可通过环境变量 `VITA_LOG_HOME` 覆盖。

## 常用命令

- 编译全仓：`mvn clean compile`
- 打包全仓：`mvn clean package -DskipTests`
- 运行全仓测试：`mvn test`
- 启动管理端：`mvn -pl vita-admin -am spring-boot:run`
- 启动调度端：`mvn -pl vita-scheduler -am spring-boot:run`
- 执行公共模块测试：`mvn -pl vita-common -am test`
- 执行业务层测试：`mvn -pl vita-service -am test`
- 单独构建部门业务及其依赖：`mvn -pl :vita-ah-waste -am clean package`；其他业务使用对应 artifactId。
- 执行代码生成：先按 `docs/development/code-generator.md` 配置类中常量，再运行 `mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`
- 执行验证码 starter 测试：`mvn -pl vita-starters/vita-captcha-spring-boot-starter -am test`

## 可选开发工具

- CodeGraph 用于本地辅助分析跨模块调用链、接口实现、影响范围和 MyBatis XML 映射，不属于应用运行时依赖或 CI 门禁。
- 团队试点固定使用 `@colbymchenry/codegraph@1.5.0`，唯一命令入口为 `./tools/codegraph/codegraph.sh`，本地索引不提交 Git。
- 安装、Codex/Claude MCP 注册、使用限制和回滚方式见 `docs/development/codegraph-integration.md`。

## 本地启动顺序

1. 准备可访问的 MySQL 和 Redis 服务。本地开发环境已验证 MySQL 8.4、Redis 8.2 的连接与认证。
2. 参考 `config/application-dev.properties.example` 创建 `config/application-dev.properties`，填写数据库、Redis 和开发 JWT 密钥。该本地配置已被 Git 忽略，不会打入 JAR；Redis 用户名留空时使用默认用户。
3. 新数据库先创建 `be_vita`，使用 `utf8mb4` 字符集和 `utf8mb4_general_ci` 排序规则，再依次导入 `sql/init/system.sql` 和 `sql/init/insert.sql`。初始化脚本包含重建表语句，只能用于新环境；已有数据的数据库不得重复导入。
4. 从后端仓库根目录启动 `vita-admin`，需要调度入口时再启动 `vita-scheduler`。默认 profile 为 `dev`，Spring Boot 会读取根目录 `config/` 下的外部开发配置；Redis 沿用数据库索引 `2`。

```bash
java -jar vita-admin/target/vita-admin-1.0.0-SNAPSHOT.jar
```

从其他目录运行时，通过 `--spring.config.additional-location=file:/配置目录/` 指定外部配置目录。生产环境显式设置 `SPRING_PROFILES_ACTIVE=prod` 并提供对应配置。本地开发也可通过 `VITA_MYSQL_PASSWORD`、`VITA_REDIS_PASSWORD`、`VITA_JWT_SECRET` 提供密钥，配置文件中的同名属性优先于这些占位符默认值。

## 基础框架裁剪

- 保留系统用户、组织、角色、菜单、权限、审计，以及验证码、文件、国际化和导入导出组件。
- 国际化保留中英文资源和自动装配能力，由应用按需引入；本次未扩展业务文案翻译。
- 已有环境下线旧 App 和工作流后，可人工执行 `sql/upgrade/20261112_remove_app_workflow.sql` 清理遗留数据；该脚本会永久删除对应账号与业务数据，执行前必须备份。新建环境无需执行。
- 仓库未内置工作流菜单；已有环境自建菜单与旧 App 专属缓存需按实际配置清理，管理端权限缓存需失效或重新登录。

## 配置与协作入口

- 当前 `application.yml` 通过 `${vita.*}` 占位符读取外部化配置，请在 profile 文件或运行环境中提供对应值。
- AI 任务的规划、Skill 路由和交付流程见 `AGENTS.md`。
- 模块边界、代码、事务、DDL、生成器及文档联动的强制约束见 `AICoding/rules/be-vita-ai-codegen-rules.md`。
- 通用 Skill、be-vita 适配层和跨项目复制方式见 `AICoding/skills/README.md`。
