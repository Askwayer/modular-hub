# Warm-Flow 一期接入设计

## 1. 目标

- 在 `vita-workflow` 内引入 Warm-Flow 后端能力。
- 在 `vita-admin` 暴露 Warm-Flow 设计器静态资源与内置接口。
- 复用 be-vita 现有 `Sa-Token` 登录态与 `Authorization: Bearer <token>` 认证链路。
- 一期不接审批闭环，不新增业务闭环接口，不改 Warm-Flow 官方表结构。

## 2. 依赖放置

- 根 `pom.xml`
  - 统一维护 `warm-flow.version=1.8.8`
  - 统一维护 `warm-flow-mybatis-plus-sb3-starter`
  - 统一维护 `warm-flow-plugin-ui-sb-web`
  - 统一维护 `warm-flow-plugin-ui-core`
- `vita-workflow/pom.xml`
  - 保留 `vita-common`、`warm-flow-mybatis-plus-sb3-starter`、`warm-flow-plugin-ui-core`、`spring-boot-starter-test`
- `vita-admin/pom.xml`
  - 新增 `vita-workflow`
  - 新增 `warm-flow-plugin-ui-sb-web`

这样可以保证：

- `vita-openapi`、`vita-scheduler` 不会通过 `vita-service` 被动暴露设计器资源。
- `vita-admin` 作为管理端启动模块，独占设计器 UI 入口并显式装配 workflow 能力。

## 3. 配置与认证

### 3.1 配置文件

`vita-admin/src/main/resources/application.yml` 通过 `spring.config.import` 引入 classpath 中由 `vita-workflow` 提供的 `application-workflow.yml`。

`application-workflow.yml` 固定配置：

```yaml
warm-flow:
  enabled: true
  ui: true
  token-name: ${vita.sa-token.token-name}
  data_source_type: mysql

vita:
  auth:
    extra-exclude-paths:
      - /warm-flow-ui/**
```

### 3.2 认证放行策略

- `SaTokenConfigure` 保留固定白名单：
  - `/auth/login`
  - `/captcha/**`
  - `/public/**`
- 新增 `vita.auth.extra-exclude-paths` 作为扩展白名单。
- 一期仅放行 `/warm-flow-ui/**`。
- `/warm-flow/**` 不放行，继续复用业务系统 token 做登录校验。

## 4. workflow 包结构

一期最小骨架落在 `vita-workflow/src/main/java/com/vita/workflow/`：

```text
com.vita.workflow
├── config/WorkflowConfiguration.java
├── config/WorkflowWebConfiguration.java
├── constant/WorkflowConstants.java
├── service/IWorkflowDefinitionService.java
├── service/impl/WorkflowDefinitionServiceImpl.java
├── handler/WorkflowPermissionHandler.java
├── service/handler/WorkflowUiHandlerSelectService.java
└── support/WorkflowLoginUserSupport.java
```

职责定义：

- `WorkflowConfiguration`
  - 用 `@Bean` 注册 Warm-Flow `PermissionHandler`
- `WorkflowWebConfiguration`
  - 拦截 `POST /warm-flow/save-json`
  - 只允许 `SUB_ADMIN` 或 `SUPER_ADMIN` 保存流程定义
- `WorkflowPermissionHandler`
  - 复用 be-vita 登录上下文
  - `getHandler()` 返回当前用户 ID
  - `permissions()` 返回 `user:<userId>` 与当前用户全部 `role:<roleCode>`
- `WorkflowLoginUserSupport`
  - 统一封装从 `LoginUserInfoModelContext` 和 `StpUtil` 读取当前登录人与角色的逻辑
- `WorkflowUiHandlerSelectService`
  - 实现 Warm-Flow 官方 `HandlerSelectService`
  - 当前只开放“角色”页签，为设计器提供固定角色项
- `IWorkflowDefinitionService` / `WorkflowDefinitionServiceImpl`
  - 提供本地 JSON 导入能力
  - 提供报销审批流程 V1 一键创建并发布能力
  - 对 `DefJson` 根节点字段做最小校验
  - 统一调用 Warm-Flow `DefService.importJson(...)`

## 5. 本地接口与不做项

新增本地 workflow API：

- `POST /system/workflow/importJson`
  - 权限码：`system:workflow:import`
  - 角色限制：`SUB_ADMIN` 或 `SUPER_ADMIN`
  - 消费原始 Warm-Flow `DefJson`
  - 仅导入流程定义，不扩展覆盖策略和自动发布
- `POST /system/workflow/reimburseApproval/create`
  - 权限码：`system:workflow:import`
  - 角色限制：`SUB_ADMIN` 或 `SUPER_ADMIN`
  - 读取 `workflow/definitions/reimburse-approval-v1.json`
  - 调用 `DefService.importJson(...)` 导入后立即 `publish(...)`

Warm-Flow 设计器内置保存接口：

- `POST /warm-flow/save-json`
  - 不区分新增和编辑，统一视为流程定义保存动作
  - 只允许 `SUB_ADMIN` 或 `SUPER_ADMIN`

仍明确不做：

- 不注册 `DataFillHandler`
- 不注册 `TenantHandler`
- 不引入角色、部门办理人展开逻辑
- 不新增新的 workflow 官方表初始化 SQL，继续使用 `sql/init/workflow.sql`

## 6. DDL 规则说明

- Warm-Flow 官方引擎表按官方 DDL 原样落库。
- be-vita 自建 workflow 扩展表仍执行仓库统一 DDL 规范：
  - `tenant_id`
  - `create_time`
  - `update_time`
  - `is_deleted`
  - `create_by_id`
  - `create_by`
  - `update_by_id`
  - `update_by`
  - `version`
  - `remark`
