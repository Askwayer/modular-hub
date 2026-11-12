# V0.1 任务清单与开发起点

[设计导航](README.md) · 所有任务均为**待实施**，归档不代表授权批量开发。

## 怎么用

先做T01，再一次选择一个任务。每项交付说明变更、执行验证、未验证项和回退条件。A编号见[验收矩阵](delivery.md)。P0是本版必做；“关键”表示位于主要交付依赖链。

| ID / 名称 | 模块、文件范围 | 前置 / 关键 | 实施与验收 |
| --- | --- | --- | --- |
| T01 测试基线 | 根pom.xml、POI starter POM；已有测试只读核对 | 无 / 是 | 根统一Surefire，优先已有3.2.5，保留headless；实际执行现有JUnit5并检查报告，不跳过失败 |
| T02 错误契约 | common/CommonResult、两个异常处理器、GlobalErrorCode与测试 | T01 / 是 | 显式HTTP映射、校验/冲突/重复/未知错误，请求标识；MockMvc验A03/A13/A14 |
| T03 运行配置 | admin application配置、AuthProperty、条件装配测试 | T01 / 是 | 不默认prod，配置分层、必要值校验、关闭非交付能力；启动验A24/A27，秘密脱敏 |
| T04 迁移基线 | sql/init参考、sql/upgrade/flyway新目录与SQL测试资源 | T01 / 是 | V1/V2、auth_version、关系约束、审计字段、旧库接管；隔离库验A22/A23 |
| T05 密码替换 | crypto适配、AuthServiceImpl、SysUserServiceImpl、AppUserServiceImpl、所需POM | T01/T04 / 是 | Argon2id统一编码校验，null更新语义，无SM4登录兜底；编码单测、两类账号回归A01/A02 |
| T06 会话与身份 | AuthTokenService、SaTokenConfigure、Trace、ContextLoader、用户状态/密码链路 | T02/T04/T05 / 是 | Token版本、逐请求有效账号、事务内递增、清上下文；真实Redis验A04–A06/A25 |
| T07 有效RBAC | StpInterfaceImpl、AuthCommonServiceImpl、菜单服务、新请求快照 | T06 / 是 | 无跨请求RBAC缓存，过滤禁用角色，info/菜单/注解统一；集成验A07/A08/A20 |
| T08 关系与审计 | 三类关联Entity/Mapper/Service、sysOperLog、XML | T04/T06 / 是 | 物理删、唯一关系、事务关键审计；MySQL验A10/A11/A26，注入审计失败 |
| T09 整体授权API | 用户/角色Controller、DTO/VO、公开Service | T07/T08 / 是 | get/setRoles、get/setAuthorization，全ID校验与行锁；验A09–A11，旧接口不绕过 |
| T10 IAM规则 | 用户/部门/角色/权限/菜单DTO、Service、VO | T04/T07 / 是 | 保护字段、管理员保护、状态、防环、引用、字符串ID；Service+DB验A09/A13/A15/A21 |
| T11 初始化与种子 | BootstrapAdminRunner、初始化测试、V3菜单权限迁移 | T05/T09/T10 / 是 | 一次性口令、默认关闭、首页/管理/演示种子；空库登录，二次初始化不覆盖 |
| T12 限流与端点 | auth策略/Redis计数、health/demo Controller与Service | T02/T03/T07 / 是 | 来源/账号限流、可信代理、live/ready、演示；集成验A12/A25 |
| T13 前端认证适配 | web-ele api/request、core/auth/user、store/auth、新权限工具 | T02，联调需T06 / 是 | 401单次跳转、登录例外、403保状态、ID类型/退出；fixture后联调A15/A18/A19 |
| T14 动态路由 | web-ele preferences、menu API、router/access/guard/core | T11/T13 / 是 | backend固定、首页、组件契约、403/404、清演示/旧偏好；E2E验A16/A17/A19/A20 |
| T15 管理页面 | web-ele system/demo页面、API、现有adapter | T09–T14 / 是 | 管理/演示、对话框、整体授权、按钮；真实联调验A09/A16/A20/A21 |
| T16 集成验收 | 后端测试资源、web-ele E2E配置/用例 | T11–T15 / 是 | 隔离MySQL/Redis、核心A01–A27自动化，数量可见且失败阻断 |
| T17 打包与恢复 | 前端deploy、新后端镜像/Compose、两仓CI | T03/T04/T12/T16 / 是 | admin/web-ele产物、版本、迁移先行、健康备份恢复；演练A28，API不回HTML |
| T18 接入契约 | Manifest样例、协议和依赖边界检查 | T02，T17前完成 / 否 | Java/Python样例，不实现注册CRUD；契约评审A29/A30 |

所有任务优先级均P0。可选日志页面、批量操作、完整OpenAPI、自动发布与组件目录不拆入此关键清单。

## 文件定位

- [根构建](../../backend/pom.xml)、[后端模块](../../backend)、[现有SQL](../../backend/sql)。
- [认证服务](../../backend/vita-service/src/main/java/com/vita/auth)、[系统领域](../../backend/vita-service/src/main/java/com/vita/system)、[Controller](../../backend/vita-admin/src/main/java/com/vita/controller)。
- [公共能力](../../backend/vita-common/src/main/java/com/vita)、[前端应用](../../frontend/apps/web-ele)、[前端部署模板](../../frontend/scripts/deploy)。
- 新类和表设计见[后端专题](backend.md)，新API和页面见[前端专题](frontend.md)。

## 依赖与回退

```text
T01 → T02 / T03 / T04
T04 → T05 → T06 → T07
T04+T06 → T08
T07+T08 → T09
T04+T07 → T10
T05+T09+T10 → T11
T02+T03+T07 → T12
T02 → T13；T11+T13 → T14
后端管理接口+T14 → T15 → T16 → T17
T02 → T18，发布前完成
```

四次交付：测试错误契约→安全登录授权→菜单管理页面→部署恢复。任务独立提交，默认不自动提交。Schema不兼容时不能只Git回退或换旧Jar，须备份恢复/前向修复。

## 第一个任务：只做T01

原因：后续修安全先要确认测试真的执行。仅检查根POM与POI覆盖，统一测试插件，保留headless。不新增生产类，不为数量增加空洞测试，不改业务代码。

建议离线命令：`mvn -o -pl vita-common,vita-service -am test`。会写target/报告；缺缓存停止报告，不自动联网。完成要求：JUnit5发现类名/数量可见，无skip或排除规避；既有失败单列，不假装通过、不顺手修。

## 第一批可并行工作（只是安排建议）

T01后，最多三个Agent，冻结接口后执行：

| 任务 | 独占文件 | 不得触碰 |
| --- | --- | --- |
| T02 | common返回体/异常及对应测试 | 登录Service、SQL、前端 |
| T04 | 新迁移与SQL验证资源 | Entity、POM、真实库 |
| T13 | web-ele请求/认证/store与单测 | Router、共享packages、SQL、锁文件 |

POM、迁移编号和共享契约由主线串行整合。T13先用fixture，联调仍依赖T06。此文不启动Agent。

## 编码前检查

- 分支、工作树、工具链与测试权限已确认；不跨任务自动修复。
- 配置不连生产，新库/旧库、旧账号策略已明确。
- HTTP、分页、ID、授权契约与文件所有权固定。
- 遵守仓库Java/数据库规则，CRUD触发时使用现有生成规范。
- 日志和测试数据无秘密；验收、未验证项、回退明确。

## 下一轮指令（可复制）

> 只实施本项目docs/03-v0.1/tasks.md中的T01。先读仓库指令、检查工作树和根POM/POI starter测试配置。优先统一仓库已有Surefire 3.2.5，保留headless，不升级Boot/JDK，不改业务、SQL、认证、前端或部署。无需新生产类或无意义测试。允许执行mvn -o -pl vita-common,vita-service -am test并产生target/报告；缺依赖时停止报告，不联网下载。核实JUnit5实际类名与数量，既有失败不跳过、不擅自修复。交付变更、验证、剩余问题和回退，不继续T02，不自动提交。

