# be-vita

`be-vita` 是基于 Spring Boot 3.5.0、JDK 17、Maven 多模块结构的后端仓库，当前同时维护管理端、AI Chat、公共基础能力、业务服务层、代码生成器和多个 Spring Boot starter。

## 文档分工

- `README.md`：仓库总览、模块结构、启动入口与常用命令。
- `CLAUDE.md`、`AGENTS.md`：面向 AI 代理和贡献者的执行摘要。
- `AICoding/rules/be-vita-ai-codegen-rules.md`：唯一详细且最高优先级的 AI 代码生成规则源。

## 当前模块结构

```text
be-vita
├── vita-common
├── vita-service
├── vita-generator
├── vita-starters
│   ├── vita-ai-spring-boot-starter
│   ├── vita-captcha-spring-boot-starter
│   ├── vita-file-spring-boot-starter
│   ├── vita-i18n-spring-boot-starter
│   └── vita-poi-spring-boot-starter
├── vita-admin
├── vita-openapi
├── vita-aichat
├── sql
│   ├── init
│   └── upgrade
└── docs
    ├── system
    ├── aichat
    └── starter
```

## 模块职责与依赖

- `vita-common`：公共基础模块，承载统一返回、异常、分页、MyBatis-Plus、Redisson、Sa-Token、工具类和基础模型。
- `vita-service`：系统业务服务层，承载认证、RBAC、系统管理等核心业务。
- `vita-admin`：管理端启动模块，入口类 `com.vita.config.VitaAdminApplication`，上下文路径 `/admin/api`。
- `vita-openapi`：公开查询接口启动模块，入口类 `com.vita.config.VitaOpenApiApplication`，上下文路径 `/openapi/api`，承载热榜等只读 public 接口。
- `vita-aichat`：AI Chat 启动模块，入口类 `com.vita.config.VitaAIChatApplication`，上下文路径 `/aichat/api`。
- `vita-scheduler`：热榜刷新触发启动模块，入口类 `com.vita.config.VitaSchedulerApplication`，上下文路径 `/scheduler/api`，承载热榜 public 刷新触发接口。
- `vita-generator`：代码生成器与模板模块，入口 `com.vita.config.CodeGenerator`。
- `vita-starters`：starter 聚合父模块，通过 `starter-*` profile 挂载各个子 starter。

主依赖链以根 `pom.xml` 为准：

```text
vita-admin   -> vita-service -> vita-common
vita-openapi -> vita-service -> vita-common
vita-scheduler -> vita-service -> vita-common
vita-aichat  -> vita-common
```

## 文档与脚本入口

- 系统设计：`docs/system/`
- AI Chat 设计：`docs/aichat/`
- Starter 设计：`docs/starter/`
- 系统初始化：`sql/init/system.sql`
- AI Chat 初始化：`sql/init/aichat.sql`
- 初始化数据：`sql/init/insert.sql`
- 升级脚本：`sql/upgrade/`
- 运行日志：`logs/vita-admin/`、`logs/vita-aichat/`

## 常用命令

- 编译全仓：`mvn clean compile`
- 打包全仓：`mvn clean package -DskipTests`
- 运行全仓测试：`mvn test`
- 启动管理端：`mvn -pl vita-admin -am spring-boot:run`
- 启动公开接口：`mvn -pl vita-openapi -am spring-boot:run`
- 启动 AI Chat：`mvn -pl vita-aichat -am spring-boot:run`
- 启动调度端：`mvn -pl vita-scheduler -am spring-boot:run`
- 执行公共模块测试：`mvn -pl vita-common -am test`
- 执行业务层测试：`mvn -pl vita-service -am test`
- 执行代码生成：`mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`
- 执行验证码 starter 测试：`mvn -f vita-starters/pom.xml -Pstarter-captcha test`

## 本地启动顺序

1. 启动 MySQL 8.x 和 Redis 7.x。
2. 执行 `sql/init/system.sql` 与 `sql/init/insert.sql`。
3. 如需 AI Chat，再执行 `sql/init/aichat.sql`。
4. 启动 `vita-admin` 或 `vita-aichat`。

## 配置与约束

- 当前 `application.yml` 通过 `${vita.*}` 占位符读取外部化配置，请在 profile 文件或运行环境中提供对应值。
- 新建表默认字段规范、分层约束、事务规则、AI Chat 和 starter 专项规则，以 `AICoding/rules/be-vita-ai-codegen-rules.md` 为准。
- 涉及认证、RBAC、日志、AI Chat、DDL 或初始化脚本的变更，必须同步更新对应 `docs/` 与 `sql/`。
