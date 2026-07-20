# Warm-Flow 二阶段通用运行时设计

## 1. 目标与边界

- 提供流程发起、我的发起、待办、已办、实例详情、流程轨迹、通过、退回和撤销的通用运行时闭环。
- `vita-common` 只声明中立工作流协议，不依赖 Warm-Flow 类型；`vita-workflow` 使用 Warm-Flow 官方 Service 实现状态变更。
- Warm-Flow 8 张引擎表不生成 Controller、Entity、Mapper 和写 Service，仅保留专用只读投影 Mapper。
- 本阶段不新增报销业务表，不实现通知、转办、委派、会签、加减签和实例状态人工修改。

## 2. 模块职责

| 模块 | 职责 |
| --- | --- |
| `vita-common` | `WorkflowPort`、命令、快照、启动上下文扩展点和提交后事件协议 |
| `vita-workflow` | Warm-Flow 适配、只读关联查询、访问校验、锁、事件发布和报销启动上下文 |
| `vita-admin` | 普通登录用户运行时接口和管理员定义、实例接口 |
| `vita-generator` | 阻止 8 张 Warm-Flow 引擎表生成业务 CRUD |

`vita-workflow` 是仓库中唯一允许使用 JOIN 的模块，且 JOIN 只能存在于专用只读 Query Mapper。其他模块、初始化 SQL、升级 SQL 和代码生成器均不得使用 JOIN。流程定义、实例和任务的写操作必须调用 Warm-Flow 官方 Service。

## 3. 公共 Java 协议

`WorkflowPort` 提供 `start`、`pass`、`rejectLast`、`rejectTo`、`cancel` 和 `getByBusinessId`。命令和返回对象不暴露 Warm-Flow DTO。

`businessId` 是长度不超过 40 的全局唯一、不可复用业务键；业务模块应使用带业务域前缀的稳定值，例如 `REIMBURSE:20260720:10001`。

启动变量按以下顺序合并，后写入值覆盖同名值：

1. 客户端变量
2. 平台变量 `initiatorId`、`initiatorDeptId`
3. 匹配 `flowCode` 的 `WorkflowStartContextProvider` 变量

流程相关办理人变量必须由 Provider 生成。`start` 创建 `TOBESUBMIT` 实例并返回首个 `taskId`，不会自动办理首个任务；申请人通过 `pass` 完成员工提交。

## 4. 普通用户接口

实际接口地址统一带应用上下文 `/admin/api`。

| 方法和路由 | 行为 |
| --- | --- |
| `GET /workflow/definitions/published` | 分页查询已发布且激活的流程定义 |
| `POST /workflow/instances/start` | 创建待提交实例并返回首个任务 |
| `GET /workflow/instances/mine` | 分页查询当前用户发起的实例 |
| `GET /workflow/instances/{instanceId}` | 查询有权访问的实例 |
| `GET /workflow/instances/{instanceId}/history` | 查询有权访问的流程轨迹 |
| `POST /workflow/instances/{instanceId}/cancel` | 发起人撤销未结束实例 |
| `GET /workflow/tasks/pending` | 按用户、角色和部门权限分页查询待办 |
| `GET /workflow/tasks/completed` | 按历史实际办理人分页查询已办 |
| `GET /workflow/tasks/{taskId}` | 查询当前用户可办理的任务 |
| `POST /workflow/tasks/{taskId}/pass` | 通过任务或提交申请 |
| `POST /workflow/tasks/{taskId}/reject-last` | 退回上一人工节点 |
| `POST /workflow/tasks/{taskId}/reject-to` | 退回定义允许的指定节点 |

实例和轨迹只允许发起人、当前候选办理人或历史实际办理人查看。任务详情和办理先执行 be-vita 归属检查，再由 Warm-Flow 执行引擎权限校验。

## 5. 管理接口与 RBAC

管理接口必须同时通过 `@SaCheckPermission` 和 `SUB_ADMIN` / `SUPER_ADMIN` 角色校验。

| 权限码 | 接口范围 |
| --- | --- |
| `system:workflow:import` | `POST /system/workflow/importJson` |
| `system:workflow:definition:view` | 定义分页和详情 |
| `system:workflow:definition:manage` | 发布、取消发布、复制和删除定义 |
| `system:workflow:instance:view` | 运行中、已完成实例分页、详情和轨迹 |

定义删除前检查是否已有实例；实例监控仅提供只读接口，不允许直接修改引擎表、任务或变量。

## 6. 并发、事务与事件

- 发起按 `vita:workflow:start:{businessId}` 加 Redis 分布式锁。
- 办理按 `vita:workflow:task:{taskId}` 加锁，撤销按 `vita:workflow:instance:{instanceId}` 加锁。
- 锁不等待，竞争失败返回 `423`；锁在 `finally` 中释放。
- `businessId` 重复校验由分布式锁内的数据库查询完成；本阶段不修改 Warm-Flow 引擎表索引。
- 状态变更事件在事务提交后发布；事务回滚不发布。
- `WorkflowChangedEvent` 不包含原始流程变量。同步监听器异常只记录日志，不改变已经提交的流程结果。

当前事件是进程内事件，不提供消息队列、Outbox 或跨进程可靠投递。

## 7. 数据库升级

新环境直接执行 `sql/init/workflow.sql`。已有环境执行：

```text
sql/upgrade/20260720_workflow_runtime_phase2_upgrade.sql
```

升级脚本只幂等补充权限记录及管理员授权，不修改 Warm-Flow 引擎表结构或索引。`sql/init/workflow.sql` 保持原始代码仓库的引擎表索引定义。

## 8. 启停与发布

- 所有自建工作流 Bean 和 Controller 受 `vita.workflow.enabled` 控制。
- 保持一期路由策略：设计器静态资源和 `/warm-flow-ui/config` 公开，`/warm-flow/**` 与 `/workflow/**` 要求登录，`/system/workflow/**` 执行管理端双重校验。
- 发布前先执行数据库权限升级脚本；历史重复业务键需由业务侧人工确认和清理。
