# 报销审批流程 V1

## 1. 流程概览

- 流程编码：`REIMBURSE_APPROVAL_V1`
- 流程路径：员工发起 -> 部门主管审批 -> 财务审批 -> 综合管理部审批 -> 总经理审批 -> 完成
- 本版仅交付 Warm-Flow 流程定义、角色准备、设计器选人协议和留档产物

## 2. 节点编码

| 节点名称 | 节点编码 | 办理人配置 |
| --- | --- | --- |
| 员工发起 | `submit_apply` | `${applicantPermission}` |
| 部门主管审批 | `dept_leader_approve` | `${deptLeaderPermission}`、`role:SUPER_ADMIN` |
| 财务审批 | `finance_approve` | `role:FINANCE_APPROVER`、`role:SUPER_ADMIN` |
| 综合管理部审批 | `general_affairs_approve` | `role:GENERAL_AFFAIRS_APPROVER`、`role:SUPER_ADMIN` |
| 总经理审批 | `general_manager_approve` | `role:GENERAL_MANAGER_APPROVER`、`role:SUPER_ADMIN` |
| 完成 | `end` | 无 |

说明：

- `submit_apply` 保留为显式用户任务，不省略。
- 超级管理员通过虚拟角色码 `SUPER_ADMIN` 并入每个审批节点办理人集合。

## 3. 办理人协议

- 用户：`user:<userId>`
- 角色：`role:<roleCode>`

运行期变量要求：

- `applicantPermission = user:<发起人ID>`
- `deptLeaderPermission = user:<发起人部门负责人ID>`

`deptLeaderPermission` 固定按以下链路解析：

- `sys_user.dept_id -> sys_dept.leader_user_id`

失败约束：

- 发起人无部门，直接失败
- 部门无 `leader_user_id`，直接失败
- 审批角色下无人时，流转时报错

## 4. 退回规则

- 员工提交/重提：`TaskService.pass`
- 返回上一级：`TaskService.rejectLast`
- 退回申请人：`TaskService.rejectAtWill(taskId, "submit_apply", ...)`

节点行为说明：

- `dept_leader_approve` 节点，“返回上一级”和“退回申请人”都会回到 `submit_apply`
- `finance_approve`、`general_affairs_approve`、`general_manager_approve` 同时保留两种退回路径
- 流程不做金额分支、不做会签、不做加签、不做抄送

## 5. 设计器选人协议

- V1 仅开放“角色”页签
- 设计器只返回以下可选项：
  - `role:FINANCE_APPROVER`
  - `role:GENERAL_AFFAIRS_APPROVER`
  - `role:GENERAL_MANAGER_APPROVER`
  - `role:SUPER_ADMIN`
- 不开放用户、部门页签
- 员工发起和部门主管节点统一通过变量表达式配置，不通过选择器选人

## 6. 角色准备

升级脚本：`sql/upgrade/20260703_reimburse_workflow_roles_upgrade.sql`

初始化三类物理角色：

- `FINANCE_APPROVER`
- `GENERAL_AFFAIRS_APPROVER`
- `GENERAL_MANAGER_APPROVER`

特殊说明：

- 不新增物理 `SUPER_ADMIN` 角色记录
- `SUPER_ADMIN` 继续表示 be-vita 的超级管理员虚拟角色语义

## 7. 产物清单

- 流程定义 JSON：`docs/system/workflow/definitions/reimburse-approval-v1.json`
- 当前说明文档：`docs/system/workflow/reimburse-approval-v1.md`

导入说明：

- 流程定义使用 Warm-Flow `DefJson` 结构留档
- 可通过 `POST /admin/api/system/workflow/importJson` 直接导入当前 JSON
- 可通过 `POST /admin/api/system/workflow/reimburseApproval/create` 一键导入并发布报销审批流程 V1
- 调用时携带 `Authorization: Bearer <token>`，并要求 `system:workflow:import` 权限和 `SUB_ADMIN` 或 `SUPER_ADMIN` 角色
- 创建前需要先执行 Warm-Flow 官方引擎表脚本 `sql/init/workflow.sql`
- 若设计器导入后发生坐标微调，以设计器重新保存结果为准，不影响节点编码、办理人协议与退回规则
