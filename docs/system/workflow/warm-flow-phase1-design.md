# Warm-Flow 一期接入设计

## 1. 目标与参考边界

- 在 `vita-workflow` 内接入 Warm-Flow 1.8.8 引擎、设计器 SPI 和本地流程定义服务。
- 仅由 `vita-admin` 暴露设计器 UI，保持 `vita-admin -> vita-workflow -> vita-service -> vita-common` 依赖方向。
- 参考 RuoYi-Vue-Plus 的模块隔离、登录态复用和条件启停方式，不复制其租户、通知、SSE、请假示例及完整任务管理业务。
- 一期只完成引擎、设计器、权限处理器、路由认证、配置和 DDL 收口，不实现业务流程发起、待办、已办和实例管理闭环。

## 2. 依赖放置

- 根 `pom.xml` 统一维护 `warm-flow.version=1.8.8` 和三个 Warm-Flow 依赖版本。
- `vita-workflow` 引入 `warm-flow-mybatis-plus-sb3-starter` 与 `warm-flow-plugin-ui-core`。
- `vita-admin` 引入 `vita-workflow` 与 `warm-flow-plugin-ui-sb-web`。
- `vita-openapi`、`vita-scheduler` 不依赖 `vita-workflow`，不会被动暴露设计器资源或接口。

## 3. 配置与启停

`vita-admin/src/main/resources/application.yml` 通过 `spring.config.import` 引入 `application-workflow.yml`。

```yaml
vita:
  workflow:
    enabled: ${VITA_WORKFLOW_ENABLED:true}

warm-flow:
  enabled: ${vita.workflow.enabled}
  ui: ${vita.workflow.enabled}
  token-name: ${vita.sa-token.token-name}
  data-source-type: mysql
```

- `vita.workflow.enabled` 是 be-vita 唯一工作流开关，默认启用。
- 引擎和设计器 UI 都映射到该开关，避免只关闭引擎但仍暴露 UI 控制器。
- `@ConditionalOnWorkflowEnabled` 应用于 be-vita 自建 workflow Bean、Advice 和控制器。
- 关闭后应用仍可启动，不注册本地 workflow 服务、控制器和 Advice，也不装配 Warm-Flow 引擎及 UI。

## 4. 路由认证矩阵

认证白名单只在 `vita-admin/src/main/resources/application.yml` 维护，profile 和 workflow 配置不得重复声明列表，避免 Spring 列表覆盖。

| 路由 | 匿名访问 | 登录用户 | 附加限制 |
| --- | --- | --- | --- |
| `/warm-flow-ui/index.html` | 允许 | 允许 | 仅设计器入口静态文件 |
| `/warm-flow-ui/css/**` | 允许 | 允许 | 静态资源 |
| `/warm-flow-ui/js/**` | 允许 | 允许 | 静态资源 |
| `/warm-flow-ui/ico/**` | 允许 | 允许 | 静态资源 |
| `/warm-flow-ui/config` | 允许 | 允许 | 设计器读取 token 配置 |
| `/warm-flow/**` | 拒绝 | 允许 | 统一执行 `StpUtil.checkLogin()`；节点办理权限由 `PermissionHandler` 校验 |
| `/system/workflow/**` | 拒绝 | 条件允许 | 必须同时满足接口权限码和 `SUB_ADMIN` / `SUPER_ADMIN` 角色 |

禁止将 `/warm-flow/**` 或 `/warm-flow-ui/**` 整体加入 `vita.auth.extra-exclude-paths`。

## 5. 运行时装配

- `WorkflowPermissionHandler` 是受工作流开关控制的 Spring `@Component`。
- `permissions()` 返回 `user:<userId>`、当前用户的 `role:<roleCode>` 与 `dept:<deptId>`。
- `getHandler()` 返回当前用户 ID，用于历史办理记录留痕。
- `WorkflowConfiguration` 在应用启动完成后校验以下组件：
  - Warm-Flow 引擎配置已初始化
  - 引擎绑定的是 be-vita `WorkflowPermissionHandler`
  - `HandlerSelectService`
  - `CategoryService`
  - `IWorkflowDefinitionService`
- Warm-Flow 1.8.8 Starter 已在 `BeanConfig.initFlow()` 内设置 `FrameInvoker` 的配置和 Bean 获取函数，be-vita 不重复绑定。
- `WorkflowDefinitionPermissionNormalizer` 继续在首次导入或首次保存时为人工节点补齐管理员角色办理权限。

## 6. 接口范围

一期只新增一个本地管理接口：

- `POST /admin/api/system/workflow/importJson`
- 权限码：`system:workflow:import`
- 角色：`SUB_ADMIN` 或 `SUPER_ADMIN`
- 行为：导入原始 Warm-Flow `DefJson`，不自动发布。

Warm-Flow 内置 `/warm-flow/**` 包含流程设计、表单与 `execute/*` 接口，统一要求登录，但一期不把这些内置接口包装成 be-vita 业务工作流 API。

当前不存在 `/system/workflow/reimburseApproval/create`，文档和权限映射不得声明该接口。

## 7. 数据库

`sql/init/workflow.sql` 包含 Warm-Flow 1.8.8 所需的 8 张表：

- `flow_definition`
- `flow_node`
- `flow_skip`
- `flow_instance`
- `flow_task`
- `flow_his_task`
- `flow_user`
- `flow_form`

be-vita 自建 `workflow_category` 继续遵循仓库审计字段规范。已有环境执行 `sql/upgrade/20260714_warm_flow_1_8_8_schema_upgrade.sql`，该脚本只幂等创建缺失的 `flow_form`，不删除或重建已有流程数据。

## 8. 第二阶段衔接

二阶段通用运行时已经独立落地，详细设计见 `docs/system/workflow/warm-flow-phase2-runtime.md`。

- `vita-common` 提供不暴露 Warm-Flow DTO 的中立工作流端口和事件协议。
- `vita-workflow` 实现定义、实例、任务、访问控制、并发锁和提交后事件编排。
- `/workflow/**` 提供普通登录用户运行时接口，service 层校验实例访问权和任务办理权。
- `/system/workflow/**` 提供定义管理和实例只读监控，执行权限码与管理员角色双重校验。
- 报销流程作为端到端验收定义接入，但仍不新增报销业务表、通知、委派、转办和会签能力。

## 9. 验收测试

- 解析实际 YAML，断言白名单不含 `/warm-flow/**` 和 `/warm-flow-ui/**`。
- 使用实际 `/admin/api/...` URL 验证匿名、普通登录用户、子管理员和超级管理员访问边界。
- 验证 `/system/workflow/importJson` 同时执行权限码和角色校验。
- 验证 `vita.workflow.enabled=false` 时条件 Bean 不注册。
- 验证引擎表生成 CRUD 不再注册，代码生成器拒绝再次生成。
- 编译全仓并执行 workflow/admin 目标测试。
