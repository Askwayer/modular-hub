# V0.1 后端、认证与数据设计

[设计导航](README.md) · 全文是拟实施方案，现状证据见[审查归档](../01-review/repository-review.md)。

## 模块与分层

保留vita-admin/service/common及既有starter，不新增七个hub模块、不改包前缀。admin是唯一运行交付入口，其他入口保留但不要求启动。

| 位置 | 职责 |
| --- | --- |
| admin/controller/auth、system | 薄Controller：HTTP、校验、权限声明 |
| admin/controller/demo、operations（拟新增） | 演示与基础健康 |
| service/auth | 登录、会话、单请求授权快照 |
| service/system各领域 | 用户、组织、角色、权限、导航与事务 |
| service/system/associate | 关系持久化 |
| service/system/sysOperLog | 关键审计 |
| common | 返回、分页、异常、追踪、密码与基础设施适配 |

沿用Mapper，不增加纯转发Repository。Entity不直接返回，DTO限定可写字段，VO负责安全投影。未来业务不依赖service/Mapper/平台库。common不接收用户、菜单和应用业务，不直接发布为跨应用SDK。

拟新增/扩展：PasswordHashService、RequestAccessService、RequestAccessSnapshot、AdminSessionGuard、现有AuthTokenService的Token版本方法、SysRoleAuthorizationDto/Vo、SysUserRolesDto、CriticalAuditService、BootstrapAdminRunner、DemoContextController/Service、PlatformHealthController。能扩展现有ContextLoader的，不再造平行框架。

## 身份认证

V0.1保留Sa-Token；立即OIDC会增加IdP、映射与退出管理成本，放V0.2决策；不自研授权服务器。

登录顺序：参数→来源/账号限流→本地图形验证码→有效账号→Argon2id验证→创建Token→Token Session保存签发安全版本→返回Token。

- PasswordHashService委托成熟crypto组件，版本由Boot BOM核实；不引入完整Security Web链。
- 编码采用成熟默认参数，在目标环境测量耗时与内存后锁定；测试错误密码、不同盐、中文和未知格式。
- 密码不trim；更新null是不修改，前端未改不发字段；非空严格校验。
- 默认使用新隔离库。必须保留旧账号时先确定重置或离线迁移，不自动解密，不增加长期SM4兜底。
- 初始化口令通过隐藏输入或受保护文件；无默认口令、无匿名初始化接口。

参考：[Spring密码存储](https://docs.spring.io/spring-security/reference/7.0/features/authentication/password-storage.html)。

## 会话安全版本

- 保留一小时绝对有效期、不增加刷新Token；不同登录不同Token。
- Header是唯一Token入口；关闭Cookie/参数读取并测试。
- 正常退出撤销当前Token。
- 用户禁用、删除、改密/重置在同一数据库事务增加auth_version。
- 新受保护请求读取最新账号：禁用、删除、Token版本不符或Token版本缺失均401。
- 版本放**每Token Session**，不能放所有会话共享的账号Session；不为旧Token自动补当前版本。
- 数据库/Redis故障不沿用无限期旧身份，返回适当503。
- 保证事务提交后开始的新请求使用新状态，不保证取消已经进入的在途请求。

## 授权与管理范围

有效链路：有效用户→用户角色→启用角色→角色权限→启用权限。菜单也过滤启用角色。

StpInterfaceImpl、info和菜单统一规则，不跨请求用RBAC缓存；单请求按loginType/userId复用快照。Trace不抢先恢复未检查的身份，结束清理ThreadLocal。异步任务不用请求ThreadLocal充当授权依据。

V0.1推荐IAM写仅超级管理员；普通用户可获只读/演示权限。Service除注解外再校验管理范围。超级管理员只能一次性初始化，普通CRUD不接收isSuperAdmin/isSystem、登录审计字段；保护唯一管理员。此范围待确认。

权限码沿用system:user:view，演示hub:demo:view；auth_tag强制等于permission_code。通配保留值不可由普通权限目录编辑。菜单、按钮、API各自判定，不互相自动授予。岗位、多租户、行级数据范围引擎不新增，data_scope字段保留但不宣称生效。

## 数据与索引

| 实体 | 设计 |
| --- | --- |
| sys_user | 保留；auth_version BIGINT默认0；单向哈希；user_name唯一；dept_id索引 |
| sys_dept | 保留；非空dept_code唯一；parent_id索引；防环、引用校验 |
| sys_role | 保留；role_code全生命周期唯一；系统标记服务端控制 |
| sys_permission | 保留；permission_code全生命周期唯一；menu_id索引 |
| sys_menu | 保留；非空路由名/内部路径唯一，parent_id索引；禁用父目录不返回子页 |
| sys_user_role | 用户/角色对唯一；反向role_id索引 |
| sys_role_permission | 角色/权限对唯一；反向permission_id索引 |
| sys_role_menu | 角色/菜单对唯一；反向menu_id索引 |
| sys_oper_log | 复用，补request_id、target_type、target_id；用户时间和请求标识索引 |
| sys_login_log | 复用，用户/时间查询索引 |
| 应用表 | V0.1不建 |

菜单与权限通过permission.menu_id展示关联，不再建第二套多对多。用户、角色、权限编码推荐删除后不复用；大小写规范化后保持唯一，已有冲突停止迁移，不自动合并。

主实体软删，授权关系物理删并以审计保历史，关系模型不再继承逻辑删除行为。旧墓碑清理只在受控迁移完成。公共字段沿用规则，tenant_id不接受客户端；version不宣称通用乐观锁，auth_version只管会话。

同一用户/角色整体授权保存锁定目标行，先校验全部ID再修改。空数组清空、字段缺失400。V0.1接受后提交覆盖，不声称有编辑冲突检测。

API的ID/父ID/关联ID均十进制字符串，前端不转Number；分页total等普通计数不因此改成字符串。

## 迁移与初始化

Flyway作为独立迁移步骤，不随admin每次启动改库。建议目录sql/upgrade/flyway，V1现有平台基线、V2安全约束、V3菜单权限种子。工具和MySQL支持版本实施时验证锁定。

- V1不含DROP、口令或真实数据；旧init保留为历史参考，不作为第二套生产权威。
- 空库V1→V2→V3；已有库备份比对后显式baseline，自动baseline关闭。
- 允许校验、禁止clean；已发布脚本不可改；MySQL DDL不假设可事务回滚。
- 旧App/工作流表不擅自删除；新库不为未交付业务增加表，通过启动测试排除隐式依赖。
- 迁移账号与运行账号分离；恢复演练先在隔离环境做。
- BootstrapAdminRunner默认关闭，显式维护模式运行；已有管理员不覆盖，正常启动不再需要初始化凭据。
- 测试夹具包含管理员、只读、演示、禁用账号，均为虚构数据。

参考：[Flyway baseline保护](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting)。

## 审计与暂缓能力

关键IAM成功变更和脱敏审计同事务提交：禁用、重置、角色分配、权限/菜单保存。普通访问和登录日志可以异步，但增加丢弃/拒绝计数，不承诺绝不丢失。不记录Token、密码/哈希、验证码或完整请求体。

保留Vben浏览器Token存储有XSS风险，仅用于可信前端、HTTPS和无第三方脚本的边界；V0.2评估BFF/HttpOnly。MFA按企业要求补；数据权限在首个真实需求前补。限流不能延期：建议开发初值账号失败5次/15分钟、可信来源30次/分钟，配置化并经测试确认，超限429。

