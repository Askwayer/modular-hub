# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

`be-vita` 是基于 Spring Boot 3.5.0、JDK 17、Maven 多模块结构的 Java 后端项目。技术栈：MyBatis-Plus、Sa-Token、Redisson、MySQL、Spring AI / Spring AI Alibaba。

## 模块结构

```text
be-vita
├── vita-common                         # 公共基础（异常、分页、MyBatis-Plus、Redisson、Sa-Token、工具类）
├── vita-service                        # 业务服务层（认证、RBAC、系统管理）
├── vita-admin                          # 管理端启动模块，上下文路径 /admin/api
├── vita-aichat                         # AI Chat 独立启动模块，上下文路径 /aichat/api
├── vita-generator                      # 代码生成器（入口：com.vita.config.CodeGenerator）
└── vita-starters                       # 自定义 starter（captcha、file、i18n、poi、ai-core）
```

依赖主链：`vita-admin -> vita-service -> vita-common`，`vita-aichat -> vita-common`

## 常用命令

| 操作 | 命令 |
| --- | --- |
| 编译全仓 | `mvn clean compile` |
| 全仓测试 | `mvn test` |
| 启动管理端 | `mvn -pl vita-admin -am spring-boot:run` |
| 启动 AI Chat | `mvn -pl vita-aichat -am spring-boot:run` |
| 测试公共模块 | `mvn -pl vita-common test` |
| 测试业务层 | `mvn -pl vita-service test` |
| 代码生成 | `mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator` |
| 测试 starter | `mvn -pl vita-starters/vita-captcha-spring-boot-starter test` |

启动顺序：MySQL -> Redis -> 执行 `sql/init/*.sql` -> 启动服务

## 架构核心

### 统一基类

- `BaseController` — 控制器基类
- `BaseEntity` — 实体基类（含审计字段）
- `BaseMapperX` — MyBatis-Plus Mapper 基类，扩展 `LambdaQueryWrapperX`
- `CommonResult<T>` — 统一返回体
- `CommonStreamResult` — SSE 流式返回体
- `PageRequest` / `PageResponse<T>` — 分页标准
- `ServiceException` + `GlobalErrorCode` — 异常体系

### 分层约定

- **Controller**：只接参、鉴权、调 Service、返回 `CommonResult`
- **Service**：业务编排、事务、跨表聚合、参数校验
- **Mapper**：单表持久化和 SQL 细节，优先 `BaseMapperX + LambdaQueryWrapperX`
- **DTO/VO**：Controller 入参用 DTO（`*CreateDto`、`*UpdateDto`、`*SearchDto`），返回用 VO（`*ListVo`、`*DetailVo`、`*PageVo`）

### 硬规则

- **禁止 join SQL**；关联查询：先查关联 ID，去重后，再查目标表
- **单表投影/计数/排序**下沉到 Mapper，Service 不要 `selectList` 后 `stream().map()`
- **事务边界**只在外部入口方法，同类 `public @Transactional` 方法禁止互调
- `/system/*` 接口默认落 `@SaCheckPermission("system:资源:动作")`
- 参数非法：`ServiceException(GlobalErrorCode.BAD_REQUEST)`；数据不存在：`ServiceException(GlobalErrorCode.NOT_FOUND)`
- Property 类统一放在 `property` 包，命名 `XxxProperty`（不用 `*Properties`）

### AI 代码生成规则

完整规则在 `aiCoding/rules/be-vita-ai-codegen-rules.md`。关键要点：
- 所有生成代码必须遵守上述分层和硬规则
- AI Chat 模块：模型调用、流式处理、状态流转沉到 `service/support/model/property`，Controller 不直接调模型 SDK
- 新增 AI Chat 接口或模型接入前，先同步 `docs/aichat/ai-chat-design.md`

## 文档与规范

- 系统设计：`docs/system/`（认证、RBAC、日志）
- AI Chat 设计：`docs/aichat/`
- OpenSpec 规格：`openspec/specs/` 及 `openspec/changes/`
- SQL 初始化：`sql/init/`，升级脚本：`sql/upgrade/`
- AI 代码生成规则：`aiCoding/rules/be-vita-ai-codegen-rules.md`
- 更多仓库指南：`AGENTS.md`

## 测试规范

- 测试代码在 `src/test/java`，与生产代码保持相同包路径
- 命名：`*Test`（单元测试），`*IT`（集成测试）
- 重点覆盖：新业务逻辑、参数校验、异常分支、starter 自动装配、AI Chat 关键链路

## 提交规范

遵循 Conventional Commits：`feat(aichat): ...`、`fix(auth): ...`、`refactor(chat): ...`
数据库、认证、RBAC、AI Chat 方案变更须同步更新 `sql/`、`docs/` 或相关配置。