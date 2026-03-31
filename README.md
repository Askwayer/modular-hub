# Vita 项目后端架构设计 (Spring Boot)

## 1. 项目简介

Vita 是一个基于 **Spring Boot** 构建的后端项目，整体采用 **模块化单体架构（Modular Monolith）** 设计。
该架构兼顾了当前阶段的开发效率、部署简单性与未来系统演进能力，适用于从 0 到 1 快速落地、后续逐步扩展的业务场景。

本项目的目标是：

- 支撑管理端与客户端两类业务场景
- 通过模块边界控制系统耦合度
- 统一基础设施能力，避免重复开发
- 为后续拆分微服务预留清晰边界

---

## 2. 核心设计思想

项目采用多模块 Maven/Gradle 结构，将系统拆分为多个职责明确的业务模块与基础模块。
所有模块统一在一个代码仓库中开发、测试和发布，但在代码组织、依赖方向和职责边界上保持清晰隔离。

### 设计原则

1. **高内聚、低耦合**
    - 每个模块聚焦单一职责
    - 禁止跨模块随意访问内部实现

2. **统一基础能力下沉**
    - 通用能力沉淀到 `vita-common` 或 `vita-starters`
    - 避免业务模块重复封装公共逻辑

3. **面向演进设计**
    - 当前以单体模式运行，降低运维成本
    - 未来可按业务边界逐步拆分为独立微服务

4. **先业务闭环，再逐步拆分**
    - 优先保证业务快速上线
    - 当某个模块出现独立扩容、独立部署、独立数据库的需求时，再考虑服务化拆分

### 这种架构的优势

- **开发效率高**：一个仓库、统一依赖管理、调试方便
- **部署成本低**：早期无需引入复杂的微服务治理体系
- **边界清晰**：模块职责明确，便于多人协作开发
- **可持续演进**：后续拆分微服务时改造成本更低

---

## 3. 项目模块划分

建议的项目结构如下：

```text
/vita-backend
|-- /vita-starters              # Spring Boot 自定义 Starter / 自动装配模块
|-- /vita-common                # 通用工具、基础配置、公共模型
|-- /vita-auth                  # 认证、授权、登录态、鉴权能力
|-- /vita-admin                 # 管理端业务模块
|-- /vita-app                   # 客户端业务模块
|
|-- pom.xml                     # 父项目 POM，统一管理依赖和模块
```

### 3.1 模块定位说明

| 模块名             | 功能简介    | 职责与说明                                                             |
|:----------------|:--------|:------------------------------------------------------------------|
| `vita-starters` | 自定义启动模块 | 封装通用基础设施能力，例如验证码、文件存储、短信、邮件、日志审计、链路追踪、限流等，可通过 Spring Boot 自动装配复用。 |
| `vita-common`   | 公共基础模块  | 存放通用返回体、异常体系、工具类、常量、枚举、基础配置、通用 VO/DTO/BaseEntity 等公共内容。           |
| `vita-auth`     | 认证授权模块  | 提供登录、登出、Token 管理、权限校验、用户上下文、角色权限能力，可为 admin 与 app 提供统一认证鉴权支持。     |
| `vita-admin`    | 管理端模块   | 面向后台管理用户，负责系统配置、运营管理、内容管理、订单管理、用户管理、报表等后台业务。                      |
| `vita-app`      | 客户端模块   | 面向外部用户，承载注册登录、商品浏览、订单交易、支付、用户中心等对外业务能力，是未来最可能独立拆分的业务域。            |

### 3.2 关于 `vita-starters` 模块

`vita-starters` 本质上属于 **基础设施能力封装模块 / 自动装配模块**，主要用于：

- 对第三方中间件做统一封装
- 对项目共性能力做可插拔沉淀
- 降低业务模块接入复杂度
- 保持配置规范一致

例如：

- `vita-captcha-spring-boot-starter`
- `vita-file-spring-boot-starter`
- `vita-log-spring-boot-starter`
- `vita-security-spring-boot-starter`

业务模块只需要引入依赖并配置参数，即可启用对应能力。

---

## 4. 推荐的分层结构

建议每个业务模块内部遵循统一分层：

```text
controller  # 接口层：接收请求、参数校验、返回响应
service     # 应用层：编排业务逻辑
manager     # 领域协调层：复杂业务聚合、跨服务调用编排（可选）
mapper      # 数据访问层：数据库操作
entity      # 实体对象
DTO/VO      # 数据传输对象 / 视图对象
```

### 分层建议

- `Controller` 只负责入参校验和结果返回，不写复杂业务逻辑
- `Service` 负责核心业务流程
- `Mapper/Repository` 只负责数据读写，不承载业务判断
- 公共能力不要散落在各业务模块，统一下沉到 `vita-common` 或 `vita-starters`

---

## 5. 模块依赖建议

推荐依赖方向如下：

```text
vita-admin  -> vita-auth -> vita-common
vita-app    -> vita-auth -> vita-common
vita-starters -> 可被所有模块依赖
```

### 依赖约束

- `vita-admin` 与 `vita-app` 之间不要直接互相依赖
- 不允许跨模块直接访问对方 `mapper` / `repository`
- 公共代码统一放在 `vita-common`
- 基础设施能力统一由 `vita-starters` 提供

这样可以有效避免模块之间形成网状依赖。

---

## 6. 本地开发说明

### 6.1 开发环境建议

建议统一以下开发环境：

- JDK：17+
- Maven：3.9+
- Spring Boot：3.x
- MySQL：8.x
- Redis：7.x
- IDE：IntelliJ IDEA
- Git：2.30+

### 6.2 启动顺序建议

本地开发时建议按以下顺序准备环境：

1. 启动 MySQL
2. 启动 Redis
3. 初始化数据库脚本
4. 启动主应用模块
5. 使用 Swagger / Apifox / Postman 进行接口联调

### 6.3 配置文件建议

建议采用多环境配置：

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

推荐做法：

- `application.yml`：基础公共配置
- `application-dev.yml`：本地开发环境
- `application-test.yml`：测试环境
- `application-prod.yml`：生产环境

敏感信息建议通过环境变量、配置中心或启动参数注入，避免明文提交到仓库。

### 6.4 数据库变更建议

- 所有表结构变更必须提交对应 SQL 脚本
- 不允许直接口头同步数据库变更
- 建议建立独立目录统一管理，例如：

```text
/sql
  /init        # 初始化脚本
  /upgrade     # 增量升级脚本
```

---

## 7. 建议的开发顺序

建议按以下顺序推进项目建设：

### 第一阶段：基础能力建设

- 搭建父工程与多模块结构
- 建立统一异常体系、返回体、日志规范
- 接入数据库、Redis、接口文档、参数校验
- 完成 `vita-common` 与 `vita-starters` 基础能力建设

### 第二阶段：认证授权建设

- 完成登录、登出、Token 认证
- 完成用户、角色、权限基础模型
- 完成 `vita-auth` 模块建设

### 第三阶段：管理端能力建设

- 构建后台管理基础功能
- 完成用户管理、角色管理、菜单管理、字典管理等
- 逐步承载运营侧业务

#### 权限模型设计建议

- 设计权限模型时建议采用 RBAC（基于角色的访问控制），并预留未来可能的权限维度扩展，例如部门范围权限等。

```text
sys_user
  └── sys_user_role
         └── sys_role
                ├── sys_role_menu
                │      └── sys_menu
                ├── sys_role_permission
                │      └── sys_permission
                └── sys_role_dept_scope
                       └── sys_dept
```

### 第四阶段：客户端能力建设

- 完成用户端注册、登录、个人中心等
- 构建面向外部用户的核心业务闭环
- 按业务增长情况决定是否拆分独立服务

---

## 8. 如何为未来微服务化做准备

即使当前采用模块化单体架构，也要在设计上尽量遵守未来可拆分原则。

### 8.1 严格遵守模块边界

- `vita-admin` 与 `vita-app` 之间不能直接访问彼此内部实现
- 所有跨模块交互必须通过公开的服务接口完成
- 禁止跨模块直接访问数据库表或数据访问层

### 8.2 API 驱动通信

可以将模块暴露的 `Service` 视为当前阶段的内部 API。
未来拆分为微服务时，仅需将本地调用替换为 RPC/HTTP 调用，调用方业务逻辑可以尽量保持不变。

### 8.3 数据隔离设计

建议从一开始就对表进行业务前缀划分，例如：

- `sys_*`：系统与权限相关表
- `admin_*`：管理端业务表
- `app_*`：客户端业务表
- `order_*`：订单域表
- `product_*`：商品域表

这样在未来拆库拆服务时更容易迁移。

### 8.4 减少共享数据库耦合

虽然当前仍是单库单体，但要避免通过“共享表 + 到处写 SQL”方式形成强耦合。
业务边界要优先通过模块 API 保持清晰。

---

## 9. Git 协作规范

为保证多人协作开发质量，建议统一 Git 分支、提交和合并规范。

### 9.1 分支命名规范

推荐采用如下分支模型：

- `main`：生产分支，保持随时可发布
- `develop`：开发主分支，日常集成开发使用
- `feature/*`：功能开发分支
- `fix/*`：普通问题修复分支
- `hotfix/*`：生产紧急修复分支
- `release/*`：预发布分支（可选）

#### 命名示例

```text
feature/user-login
feature/order-create
fix/app-user-query
hotfix/payment-callback
release/1.0.0
```

### 9.2 提交规范

推荐使用统一提交信息格式：

```text
<type>(<scope>): <subject>
```

常用 `type`：

- `feat`：新增功能
- `fix`：修复问题
- `refactor`：重构代码
- `docs`：文档更新
- `style`：格式调整，不影响逻辑
- `test`：测试相关
- `chore`：构建、配置、脚本等杂项修改
- `perf`：性能优化
- `build`：构建系统或依赖调整
- `ci`：CI/CD 配置变更

#### 提交示例

```text
feat(auth): 新增后台用户登录功能
fix(app): 修复用户中心头像为空导致报错问题
docs(readme): 完善项目开发文档与 Git 规范
refactor(common): 抽取统一响应结果封装
chore(sql): 新增初始化脚本目录结构
```

### 9.3 提交要求

- 一次提交只做一类事情，避免“大杂烩提交”
- 提交前确保代码可编译、可运行、关键流程可验证
- 不要把临时调试代码、无用日志、IDE 配置随意提交到仓库
- 不要直接提交密码、密钥、证书等敏感信息

### 9.4 合并请求规范

发起合并请求（Merge Request / Pull Request）前应确保：

- 代码已完成自测
- 涉及数据库变更时已附带 SQL 脚本
- 涉及配置变更时已说明影响范围
- 涉及接口变更时已同步更新接口文档
- 说明本次改动内容、风险点、验证方式

### 9.5 推荐协作流程

```text
1. 从 develop 拉取最新代码
2. 新建 feature/fix 分支进行开发
3. 完成开发后进行自测
4. 推送远程分支并发起 MR/PR
5. 通过 Code Review 后合并到 develop
6. 测试通过后根据发布计划合并到 main
```

### 9.6 禁止事项

- 禁止直接在 `main` 分支开发
- 禁止未经评审直接合并核心分支
- 禁止将未验证代码直接发布生产
- 禁止提交与当前需求无关的大量改动

---

## 10. 代码规范建议

### 10.1 包结构统一

建议统一包路径风格，例如：

```text
com.vita.admin
com.vita.app
com.vita.auth
com.vita.common
```

### 10.2 命名规范

- 类名使用大驼峰：`UserLoginService`
- 方法名使用小驼峰：`queryUserInfo`
- 常量全部大写下划线：`DEFAULT_PAGE_SIZE`
- 数据库表名建议使用小写下划线：`app_user`
- 字段名保持统一风格，避免同义字段多种命名方式

### 10.3 日志规范

- 关键业务流程必须有日志
- 异常日志要带上下文关键信息
- 禁止输出密码、Token、身份证号、银行卡号等敏感数据

---

## 11. README 维护建议

为了让 README 真正服务于团队协作，建议长期维护以下内容：

- 项目简介
- 模块说明
- 技术栈说明
- 启动方式
- 环境要求
- 配置说明
- 数据库脚本说明
- Git 规范
- 发布流程说明
- 常见问题说明

README 不应只作为“项目介绍”，还应成为新成员接手项目时的第一份落地文档。

---

## 12. 总结

Vita 项目当前最适合采用 **模块化单体架构**：

- 既能保障当前阶段快速交付
- 又能通过清晰的模块边界为未来扩展预留空间
- 通过统一基础设施封装与 Git 协作规范，可以提升团队开发效率与代码质量

当 `vita-app` 或其他业务域出现明显的独立扩容、独立部署、独立数据库需求时，再逐步推进微服务拆分，会比一开始直接上微服务更加稳妥。

---

## 13. 后续可继续补充的内容

后续建议继续在 README 中增加以下章节：

- 技术栈清单（Spring Boot、MySQL、Redis、MQ、对象存储等）
- 接口文档访问地址
- 本地运行示例
- Docker 部署方式
- CI/CD 发布流程
- 数据库设计规范
- 权限模型说明（RBAC）
- 代码提交模板 / PR 模板

如需进一步规范化，也可以拆分出：

- `README.md`：项目总览
- `CONTRIBUTING.md`：开发与提交流程
- `CHANGELOG.md`：版本变更记录
- `docs/architecture.md`：详细架构设计文档

---

## 14. 代码生成器（vita-generator）

`vita-generator` 已支持按模块生成代码，可通过 JVM 参数覆盖默认值（默认兼容旧配置）：

- `vita.codegen.targetModule`：领域代码模块目录（默认：`vita-service`）
- `vita.codegen.controllerModule`：控制器代码模块目录（默认：`vita-admin`）
- `vita.codegen.businessModule`：业务模块标识（用于父包、路由前缀、mapper 目录）
- `vita.codegen.domainModule`：领域模块名（用于 `com.vita.<businessModule>.<domainModule>`）
- `vita.codegen.basePackage`：基础包（示例：`com.vita`）
- `vita.codegen.routePrefix`：接口路由前缀（默认同 `businessModule`）

默认执行：

```bash
mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator
```

自定义模块执行示例：

```bash
mvn -pl vita-generator -DskipTests exec:java \
  -Dexec.mainClass=com.vita.config.CodeGenerator \
  -Dvita.codegen.targetModule=vita-service \
  -Dvita.codegen.controllerModule=vita-admin \
  -Dvita.codegen.businessModule=user \
  -Dvita.codegen.domainModule=member \
  -Dvita.codegen.basePackage=com.vita \
  -Dvita.codegen.routePrefix=user
```

输出目录示例：

- 领域代码（entity/service/mapper/dto/vo/model）：
  `<targetModule>/src/main/java/com/vita/<businessModule>/<domainModule>/...`
- 控制器代码（controller）：`<controllerModule>/src/main/java/com/vita/controller/<businessModule>/...`
- XML：`<targetModule>/src/main/resources/mapper/<businessModule>/...`

### 14.1 `mybatis-plus-v1` 与 `mybatis-plus-v2` 对齐映射

- 分层结构：`controller` / `service` / `serviceImpl` / `dto` / `vo` / `model` 与 v2 保持一致。
- Controller 方法：`create`、`update`、`delete`、`getDetail`、`list`、`page`。
- Service 方法：`Long create(...)`、`void update(...)`、`void delete(Long id)`、`DetailVo get(Long id)`、
  `List<ListVo> list(...)`、`PageResponse<PageVo> page(...)`。
- DTO/VO 规则：`CreateDto`、`UpdateDto`、`SearchDto`、`DeletedDto`、`DetailVo`、`ListVo`、`PageVo`；分页查询关键字统一为
  `keyword`。
- Model 规则：`<Entity>Model extends <Entity>`。

### 14.2 注释与依赖规范

- 生成模板中的对象、字段、公开方法必须带 JavaDoc。
- `serviceImpl` 中每个 `@Override` 方法必须包含用途、参数和返回值说明。
- 清理未使用 import，避免历史遗留冗余依赖引用。
- 对 `java.*` 类型字段，模板直接输出全限定类型，避免缺失导入导致编译失败。
- 目标业务模块若使用 Lombok 注解（`@Data`、`@EqualsAndHashCode`），需在模块 `pom.xml` 显式引入：

```xml

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```
