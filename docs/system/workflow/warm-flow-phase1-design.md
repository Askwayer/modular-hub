# Warm-Flow 一期接入设计

## 1. 目标

- 在 `vita-service` 内引入 Warm-Flow 后端能力。
- 在 `vita-admin` 暴露 Warm-Flow 设计器静态资源与内置接口。
- 复用 be-vita 现有 `Sa-Token` 登录态与 `Authorization: Bearer <token>` 认证链路。
- 一期不接审批闭环、不新增本地 workflow 业务 API、不落官方工作流表 SQL。

## 2. 依赖放置

- 根 `pom.xml`
  - 统一维护 `warm-flow.version=1.8.8`
  - 统一维护 `warm-flow-mybatis-plus-sb3-starter`
  - 统一维护 `warm-flow-plugin-ui-sb-web`
- `vita-service/pom.xml`
  - 仅保留 `warm-flow-mybatis-plus-sb3-starter`
- `vita-admin/pom.xml`
  - 新增 `warm-flow-plugin-ui-sb-web`

这样可以保证：

- `vita-openapi`、`vita-scheduler` 不会通过 `vita-service` 被动暴露设计器资源。
- `vita-admin` 作为管理端启动模块，独占设计器 UI 入口。

## 3. 配置与认证

### 3.1 配置文件

`vita-admin/src/main/resources/application.yml` 通过 `spring.config.import` 引入 `application-workflow.yml`。

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

一期最小骨架落在 `vita-service/src/main/java/com/vita/workflow/`：

```text
com.vita.workflow
├── config/WorkflowConfiguration.java
├── handler/WorkflowPermissionHandler.java
└── support/WorkflowLoginUserSupport.java
```

职责定义：

- `WorkflowConfiguration`
  - 用 `@Bean` 注册 Warm-Flow `PermissionHandler`
- `WorkflowPermissionHandler`
  - 复用 be-vita 登录上下文
  - `getHandler()` 返回当前用户 ID
  - `permissions()` 一期只返回当前用户标识
- `WorkflowLoginUserSupport`
  - 统一封装从 `LoginUserInfoModelContext` 取用户 ID 的逻辑

## 5. 一期明确不做

- 不新增 `/system/workflow/**` 本地接口
- 不注册 `DataFillHandler`
- 不注册 `TenantHandler`
- 不注册 `HandlerSelectService`
- 不引入角色、部门办理人展开逻辑
- 不新增 workflow 官方表初始化 SQL

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
