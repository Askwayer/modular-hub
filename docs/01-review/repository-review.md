# 工程清单与主要发现

[返回导航](../README.md) · 定位：第一轮审查事实及第二轮补充，不是实施记录。

## 工程结构

```text
backend/
  vita-admin       管理端入口和 Controller
  vita-app         用户端认证与 OAuth 入口
  vita-openapi     审查时主源码仅启动类
  vita-scheduler   审查时主源码仅启动类
  vita-service     认证、系统管理、App 用户等共享业务
  vita-common      Web、数据库、缓存、认证、日志等基础设施
  vita-workflow    Warm-Flow、运行时与报销流程
  vita-generator   代码生成
  vita-starters    验证码、文件、国际化、Excel
  sql/             初始化与升级脚本
  docs/            既有后端设计和开发文档
frontend/
  apps/web-ele     当前实际管理前端
  apps/backend-mock
  packages/        共享 UI、请求、布局、状态和工具
  internal/        构建、类型和质量配置
  playground/
  scripts/
```

后端直接 Maven 依赖静态解析未发现环；这不代表 Bean 或业务调用不存在循环。主要链路为 admin → service → common，admin → workflow → service，其他入口 → service。多个入口共享 service 和配置中的业务数据库，不具备天然数据自治。

证据：[根 POM](../../backend/pom.xml#L37)、[管理端装配](../../backend/vita-admin/src/main/java/com/vita/config/VitaAdminApplication.java#L19)。

## 技术栈快照

| 技术 | 审查确认 |
| --- | --- |
| Java / Boot | 17 / 3.5.0 |
| 认证 | Sa-Token 1.44.0，JWT Simple + Session/Redis；非 Spring Security Web |
| ORM | MyBatis-Plus 3.5.12、XML Mapper |
| 数据源 | dynamic-datasource 4.3.1、HikariCP 5.0.1 |
| MySQL 驱动 | 8.0.32，不代表服务端版本 |
| Redis 客户端 | Redisson 3.50.0，另有 Spring Data Redis |
| 工作流 | Warm-Flow 1.8.8，默认关闭 |
| 文件 / Excel | 本地与 MinIO（SDK 8.2.1）/ EasyExcel 4.0.3 |
| Vben | 5.7.0，实际应用 Element Plus 版 |
| Vue / TS / Vite | web-ele 锁定 Vue 3.5.33 / TS 6.0.3 / Vite 8.0.10 |
| UI / 状态 / 路由 | Element Plus 2.13.7 / Pinia 3.0.4 / Vue Router 锁定 5.0.6 |
| 包管理 | pnpm 10.33.0、workspace、Turborepo |
| Node 声明 | ^20.19.0 或 ^22.18.0 或 ^24.0.0 |

依据：[后端版本](../../backend/pom.xml#L15)、[前端声明](../../frontend/package.json)、[依赖锁定](../../frontend/pnpm-lock.yaml)。声明、锁定版本均不代表安装或运行验证通过。

## 值得保留的能力

- Controller / Service / Mapper 分层、DTO/VO、统一返回和分页。
- 用户、部门、角色、权限、菜单及关联模型。
- 登录、验证码、IP 封禁检查、登录审计。
- App OAuth 的 state、PKCE 编排和一次性票据思路；它不是平台向业务应用提供的 OIDC 服务。
- 请求追踪、日志脱敏、文件能力、生成器与已有测试。
- 角色跨表操作事务、工作流提交后事件与延迟释放锁的边界意识。

## 结构性发现

| 发现 | 影响与后续处理 |
| --- | --- |
| service 汇集系统和 App 业务 | 未来部门业务继续加入会形成共同发布中心；限定其为平台核心 |
| common 携带大量基础设施依赖 | 不宜直接作为所有独立应用的 SDK；出现真实消费者后提取窄契约 |
| 全包扫描与广泛 Mapper 扫描 | 依赖约定维持边界；通过条件装配与启动测试缩小交付面 |
| 工作流包含报销业务 | 通用运行时与报销领域未来可能分离，V0.1 不展开 |
| 规则要求非工作流业务下沉 service | 只适用于平台仓库，不应扩展到所有独立应用 |

## 安全与可靠性发现

| 编号 | 发现与证据 | 归档后的处置 |
| --- | --- | --- |
| S1 | [prod 配置](../../backend/vita-admin/src/main/resources/application-prod.yml#L14)存在明文凭据和签名材料 | V0.1 外置；有效性未验证，若已用须轮换 |
| S2 | [管理密码校验](../../backend/vita-service/src/main/java/com/vita/auth/service/impl/AuthServiceImpl.java#L272)使用固定密钥 SM4；App 同类 | V0.1 改单向哈希，旧账号策略待确认 |
| S3 | [权限查询](../../backend/vita-service/src/main/java/com/vita/auth/service/impl/AuthCommonServiceImpl.java#L75)未完整过滤角色启用状态 | V0.1 修复，菜单同步过滤 |
| S4 | [权限接口](../../backend/vita-service/src/main/java/com/vita/auth/StpInterfaceImpl.java#L38)优先用 Session 缓存；撤权链路未见完整失效 | V0.1 单请求快照和实时有效性检查 |
| S5 | [认证异常](../../backend/vita-common/src/main/java/com/vita/auth/handler/SaTokenExceptionHandler.java#L66)体内401与 HTTP 状态不一致；前端看 HTTP401 | V0.1 统一错误契约 |
| S6 | [error(String)](../../backend/vita-common/src/main/java/com/vita/core/CommonResult.java#L73)返回 code=200 | V0.1 修复，覆盖参数校验路径 |
| S7 | [MyBatis 配置](../../backend/vita-common/src/main/java/com/vita/mybatis/config/MybatisConfig.java#L24)只有分页；字段不等于租户/乐观锁保障 | 不宣称隔离；通用引擎暂缓 |
| S8 | [默认线程池](../../backend/vita-common/src/main/java/com/vita/async/config/ThreadPoolConfig.java#L30)使用 DiscardPolicy，审计走该池 | V0.1 关键 IAM 成功审计事务内记录；普通异步日志可观测 |
| S9 | [IP 获取](../../backend/vita-common/src/main/java/com/vita/utils/web/ip/IpUtils.java#L44)信任转发头 | 限制后端直连、清洗头并定义可信代理 |
| S10 | [工作流锁](../../backend/vita-workflow/src/main/java/com/vita/workflow/runtime/support/WorkflowDistributedLock.java#L27)固定60秒租期 | 超长事务失锁风险待压测；启用工作流前处理 |
| S11 | [提交后事件](../../backend/vita-workflow/src/main/java/com/vita/workflow/runtime/support/WorkflowAfterCommitEventPublisher.java#L51)失败只记录日志 | 按可靠性需求补重试/补偿，V0.1不启用 |
| S12 | [前端存储](../../frontend/packages/stores/src/setup.ts#L42)持久化 Token | 前端加密不防 XSS；V0.2评估 BFF/HttpOnly |
| S13 | [授权关系唯一索引](../../backend/sql/init/system.sql#L329)包含布尔删除位 | 重复授权撤权可能冲突；改关系物理删除并做 MySQL 测试 |

CORS 反射 Origin 的函数存在，但调用路径未确认，不将其写成已生效漏洞。

## 前端集成发现

- 已适配 username→userName、tokenValue、content、Bearer 和 /auth/routers。
- [默认权限模式](../../frontend/packages/@core/preferences/src/config.ts#L5)为 frontend，应用未覆盖，不能认为后端菜单默认启用。
- [本地 glob 组件映射](../../frontend/apps/web-ele/src/router/access.ts#L17)不是远程应用加载。
- 后端超级管理员返回通配权限，前端共享权限判断精确匹配；需应用层对齐。
- [角色 Controller](../../backend/vita-admin/src/main/java/com/vita/controller/system/SysRoleController.java#L210)缺完整菜单授权入口。
- [用户更新 DTO](../../backend/vita-service/src/main/java/com/vita/system/sysUser/dto/SysUserUpdateDto.java#L39)空字符串密码与 Service 的不修改语义不一致。
- 普通业务码包含900、1001等，不能直接作为 HTTP 状态。

## 测试、部署与未验证项

此前统计后端37个Java测试文件，前端36个测试文件；前端主要覆盖共享包，未发现 web-ele 应用测试。测试文件数不是覆盖率或执行数量。

根 POM 未统一配置 Surefire，POI starter 配置3.2.5；JUnit5实际发现情况待执行验证。前端 Docker 模板部署 playground；Nginx模板缺平台 API代理。GitHub Actions主要用于飞书通知，未见完整GitLab CI、后端镜像和Compose闭环。

未验证：实际数据库结构/版本、Redis版本、凭据有效性、工具链、构建与测试结果、生产代理、企业GitLab/Nexus能力、联调结果及容量。既有文档存在设计目标与源码进度不同的情况，应以代码和后续可执行验证为准。

