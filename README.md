# be-vita

`be-vita` 是一个基于 Spring Boot 3.x、JDK 17、Maven 多模块结构的后端项目。当前仓库同时维护管理端能力、AI Chat 模块、公共基础设施、业务服务层、代码生成器和多个 starter。

## 当前模块结构

```text
be-vita
├── vita-common                         # 公共基础能力
├── vita-service                        # 业务服务层
├── vita-generator                      # 代码生成器
├── vita-starters                       # 自定义 starter 聚合模块
│   ├── vita-captcha-spring-boot-starter
│   ├── vita-file-spring-boot-starter
│   ├── vita-i18n-spring-boot-starter
│   └── vita-poi-spring-boot-starter
├── vita-admin                          # 管理端启动模块
├── vita-aichat                         # AI Chat 独立启动模块
├── sql
│   ├── init
│   └── upgrade
└── docs
    ├── system
    └── aichat
```

## 模块职责

| 模块 | 说明 | 关键入口 / 备注 |
| --- | --- | --- |
| `vita-common` | 公共基础能力，包含异常体系、分页、MyBatis-Plus、Redisson、Sa-Token、工具类、基础模型等 | 被 `vita-service`、`vita-admin`、`vita-aichat` 复用 |
| `vita-service` | 系统业务服务层，承载认证、RBAC、系统管理等核心业务实现 | 当前依赖 `vita-common` |
| `vita-generator` | 代码生成器与模板 | 默认入口 `com.vita.config.CodeGenerator` |
| `vita-starters` | 自定义 starter 聚合 | 目前包含 captcha、file、i18n、poi 四个 starter |
| `vita-admin` | 管理端启动模块 | 启动类 `com.vita.config.VitaAdminApplication`，上下文路径 `/admin/api` |
| `vita-aichat` | AI Chat 启动模块 | 启动类 `com.vita.config.VitaAIChatApplication`，上下文路径 `/aichat/api` |

## 当前依赖关系

当前仓库实际依赖关系以 `pom.xml` 为准，主链路如下：

```text
vita-admin  -> vita-service -> vita-common
vita-aichat -> vita-common
vita-starters -> 被业务模块按需依赖
```

约束：

- 统一使用 `com.vita.*` 包前缀。
- `vita-admin` 只承载管理端 Web 启动与控制器，不直接下沉业务实现。
- `vita-aichat` 是独立启动模块，不应依赖 `vita-admin` 内部实现。
- 跨模块复用的公共能力继续沉淀到 `vita-common` 或对应 starter。
- `application` 配置文件直接配置不要引入环境变量，保持模块独立可启动。

## AI Chat 模块现状

`vita-aichat` 已经落了基础骨架：

- 三张核心表：`ai_conversation`、`ai_message`、`ai_conversation_summary`
- 三组基础实体：`AiConversation`、`AiMessage`、`AiConversationSummary`
- 三组 CRUD 控制器：`/aichat/aiConversation/**`、`/aichat/aiMessage/**`、`/aichat/aiConversationSummary/**`

这些 CRUD 接口当前更适合作为脚手架或后台维护接口，不作为正式 Chat 业务接口的长期契约。正式设计见 [docs/aichat/ai-chat-design.md](docs/aichat/ai-chat-design.md)。

本轮设计约定：

- 首个接入模型为 `Qwen`
- 对接协议采用 `OpenAI-compatible`
- 业务层通过统一网关抽象接入模型，避免直接耦合厂商 SDK

## 文档与脚本

关键文档与初始化脚本如下：

- 系统设计文档：`docs/system/`
- AI Chat 设计文档：`docs/aichat/ai-chat-design.md`
- 系统初始化脚本：`sql/init/system.sql`
- AI Chat 初始化脚本：`sql/init/aichat.sql`
- 初始化数据脚本：`sql/init/insert.sql`
- 增量升级脚本目录：`sql/upgrade/`

日志目录：

- 管理端：`logs/vita-admin/`
- AI Chat：`logs/vita-aichat/`

## 本地开发

环境要求：

- JDK 17+
- Maven 3.9+
- MySQL 8.x
- Redis 7.x

常用命令：

- 编译全仓：`mvn clean compile`
- 全仓测试：`mvn test`
- 管理端启动：`mvn -pl vita-admin -am spring-boot:run`
- AI Chat 启动：`mvn -pl vita-aichat -am spring-boot:run`
- 公共模块测试：`mvn -pl vita-common -am test`
- 代码生成：`mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`
- Starter 测试：`mvn -pl vita-starters/vita-captcha-spring-boot-starter test`

推荐本地启动顺序：

1. 启动 MySQL
2. 启动 Redis
3. 执行 `sql/init/system.sql`、`sql/init/insert.sql`
4. 如需 AI Chat，再执行 `sql/init/aichat.sql`
5. 启动 `vita-admin` 或 `vita-aichat`

## 开发约束

- Controller 仅负责参数接收、登录态获取、统一返回封装。
- 业务逻辑放在 Service，数据访问放在 Mapper。
- 文档、SQL、配置变更必须同步提交，不只写在 commit message 中。
- 涉及认证、RBAC、日志、AI Chat 的方案调整，优先更新 `docs/system/` 或 `docs/aichat/`。
