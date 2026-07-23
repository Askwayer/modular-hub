# Warm-Flow 三阶段 Controller 聚合设计

## 1. 目标

- 将通用流程定义、实例和任务 HTTP 边界聚合到 `vita-workflow`。
- 保留现有 `/workflow/**`、`/system/workflow/**` 路由和请求响应结构。
- 默认实现全部启用，不增加 Controller 级 YAML 开关。
- 允许业务项目提供同接口 Bean 自动替换整组默认 Controller。
- 报销属于具体业务，普通端和管理端 Controller 继续位于 `vita-admin`。

## 2. Controller 契约

| 接口 | 默认实现 | 能力 |
| --- | --- | --- |
| `WorkflowDefinitionControllerApi` | `DefaultWorkflowDefinitionController` | 已发布定义、定义管理、JSON 导入 |
| `WorkflowInstanceControllerApi` | `DefaultWorkflowInstanceController` | 发起、我的流程、详情、轨迹、撤回、实例监控 |
| `WorkflowTaskControllerApi` | `DefaultWorkflowTaskController` | 待办、已办、任务详情、通过、退回 |

接口放在 `com.vita.workflow.controller.api`，完整声明 Spring MVC 路由、参数绑定和校验。
默认实现放在 `com.vita.workflow.controller.impl`，只负责鉴权、请求转换、调用
`WorkflowRuntimeService` 和返回 `CommonResult`。

`WorkflowControllerConfiguration` 通过 Spring Boot AutoConfiguration 注册默认实现，并使用
`@ConditionalOnMissingBean` 检测自定义接口 Bean。`vita.workflow.enabled=false` 时三组 Controller
均不注册；总开关缺省为 `true`。

## 3. 自定义方式

自定义整组 Controller 时，声明实现对应 API 的 `@RestController` Bean。需要只调整一个方法时，
可以继承默认实现并覆盖目标方法；自定义 Bean 存在后，同组默认 Bean 自动退让，不需要修改 YAML。

Sa-Token 1.44.0 使用 Java 反射直接读取实现方法注解，不会自动读取接口方法上的
`@SaCheckPermission`。因此覆盖 `/system/workflow/**` 方法时，必须在覆盖方法上重新声明权限注解，
并调用 `WorkflowAdminAccessChecker.check()` 保留管理员角色校验。覆盖带重复提交保护的方法时，也必须
重新声明 `@RepeatSubmit`。

## 4. 权限边界

系统接口继续复用现有权限码：

- 定义分页、详情：`system:workflow:definition:view`
- 发布、取消发布、复制、删除：`system:workflow:definition:manage`
- JSON 导入：`system:workflow:import`
- 实例分页、详情、轨迹：`system:workflow:instance:view`
- 报销管理查询：`system:workflow:reimbursement:view`
- 报销状态重同步：`system:workflow:reimbursement:resync`

所有系统接口同时要求 `SUB_ADMIN` 或 `SUPER_ADMIN`。普通 `/workflow/**` 接口不增加功能权限码，
继续依赖登录态、发起人、当前候选人和历史参与人校验。

## 5. 验收

- 三组默认 Controller 在工作流总开关默认值下全部注册。
- 自定义接口 Bean 存在时，同组默认 Bean 不注册且不存在重复路由。
- 接口继承的 Spring MVC 路由能够被 `RequestMappingHandlerMapping` 识别。
- `/system/workflow/**` 默认实现方法直接声明正确的权限码和管理员角色校验。
- `RepeatSubmitAspect` 能匹配 `com.vita..controller..*Controller`，迁移后保护行为不丢失。
- 报销两个 Controller 仍位于 `vita-admin`，业务 Service 和状态机行为不变。
