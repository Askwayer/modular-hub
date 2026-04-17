# be-vita AI 代码生成 Rules

## 1. 文档目标

本文档用于约束 `be-vita` 仓库中的 AI 代码生成行为，适用于 `Codex`、`Qwen` 以及其他基于 Prompt 的代码生成工具。

目标不是描述抽象编码风格，而是沉淀“在当前仓库中生成代码时必须遵守的工程规则”，避免生成结果偏离已有实现方式。

本文档覆盖以下模块：

- `vita-admin`
- `vita-service`
- `vita-common`
- `vita-aichat`
- `vita-starters`
- `vita-generator`

本文档是全项目 AI 代码生成的主规则源。仓库入口摘要放在根目录 [AGENTS.md](../../AGENTS.md)，但详细规则以本文为准。

## 2. 项目上下文

### 2.1 模块与依赖主链

当前仓库是 Maven 多模块后端项目，主链路保持：

- `vita-admin -> vita-service -> vita-common`
- `vita-aichat -> vita-common`

`vita-starters` 为通用 Starter 能力模块，主要用于封装公共自动装配能力、基础组件配置以及可复用的扩展能力。

`vita-generator` 为代码生成器与模板模块，主要用于维护代码生成逻辑、模板文件以及相关生成配置。

以上两个模块均通过 `pom.xml` 按需引入。若未被业务模块依赖，则在项目运行、Debug 调试以及 Maven 构建过程中不会参与执行，也不会影响主业务模块的启动和构建。
- AI 生成代码时必须遵守以下依赖边界：

- `vita-admin` 只依赖 `vita-service` 和 `vita-common`
- `vita-service` 不依赖 `vita-admin`
- `vita-aichat` 不依赖 `vita-admin`
- 业务代码不要跨层直接访问别的模块内部实现

### 2.2 统一技术基线

- 包前缀统一为 `com.vita.*`
- Web 返回结构统一使用 `CommonResult<T>`
- 分页统一使用 `PageRequest` / `PageResponse<T>`
- MyBatis-Plus 查询统一优先 `BaseMapperX` 与 `LambdaQueryWrapperX`
- RBAC 管理端接口统一使用 `@SaCheckPermission`
- 事务默认使用 Spring `@Transactional(rollbackFor = Exception.class)`

## 3. 全项目共用硬规则

### 3.1 分层规则

- Controller 只负责接参、鉴权、调用 Service、返回 `CommonResult`
- Service 负责业务编排、参数校验、事务控制、跨表聚合
- Mapper 负责持久化和单表 SQL 细节
- Entity 不承担 Controller 入参语义，不把 Web 校验规则写进 Entity

禁止出现以下情况：

- Controller 直接调用 Mapper
- Controller 中写复杂业务判断或跨表组装
- Service 中写大段 SQL 语义字符串或代替 Mapper 承担单表投影细节
- 模块间绕过 Service 访问内部实现

### 3.2 命名规则

- 类名使用 `PascalCase`
- 方法、字段使用 `camelCase`
- 常量使用 `UPPER_SNAKE_CASE`
- 后缀固定：
  - `*Controller`
  - `*Service`
  - `*ServiceImpl`
  - `*Mapper`
  - `*CreateDto`
  - `*UpdateDto`
  - `*SearchDto`
  - `*DetailVo`
  - `*ListVo`
  - `*PageVo`
  - `*OptionVo`
  - `*Property`

后续 AI 生成配置类时，统一使用单数 `XxxProperty`，不要生成 `*Properties`。

### 3.3 返回体与分页规则

- 普通接口返回 `CommonResult<T>`
- 分页接口返回 `CommonResult<PageResponse<*PageVo>>`
- 列表接口返回 `CommonResult<List<*ListVo>>`
- `SearchDto` 默认继承 `PageRequest`
- `UpdateDto` 默认继承 `CreateDto` 并补 `id`

不要生成以下风格：

- 自定义另一套 `Result` / `Response` 返回体
- 分页字段使用 `pageNo`、`size`、`records` 等与仓库不一致的命名
- 在分页接口中直接返回 MyBatis-Plus `Page`

### 3.4 持久化与 SQL 规则

- 新代码查询优先 `BaseMapperX + LambdaQueryWrapperX`
- `Wrapper` 条件优先用 `eqIfPresent`、`likeIfPresent`、`inIfPresent`
- 单表投影、单表排序、单表去重、单表计数下沉到 Mapper
- 跨表查询统一由 Service 编排

强制限制：

- 禁止 `join` SQL
- 关联查询统一遵循“先查关联 ID，去重后，再查目标表”
- 禁止在 Service 中先 `selectList` 实体再做本应由 Mapper 完成的单表列投影

逻辑删除规则：

- 基于 MyBatis-Plus Wrapper 的普通查询，不重复手写 `is_deleted = 0`
- 只有 XML / 手写 SQL 才显式补逻辑删除过滤

### 3.5 事务规则

- 事务边界只放在外部可调用入口方法
- `create/update/delete/batch` 之类的写接口需要事务时，直接标注在对外方法上
- 同一 Service 中多个 `public @Transactional` 方法不能互相直接调用
- 共用逻辑统一提取为 `private` 方法

禁止出现：

- `delete()` 调 `deleteBatch()` 这种同类内 public 互调
- 为了事务生效引入 `self` 注入或循环依赖
- 事务传播依赖 `AopContext.currentProxy()` 作为默认方案

### 3.6 参数校验与异常规则

- 入参基础校验放在 DTO 上，使用 `jakarta.validation`
- Service 层必须做空值、关键 ID、关键上下文校验
- 参数非法统一抛 `ServiceException(GlobalErrorCode.BAD_REQUEST)`
- 数据不存在统一抛 `ServiceException(GlobalErrorCode.NOT_FOUND)`

不要生成：

- 返回 `null` 表示失败
- 直接 `throw new RuntimeException("xxx")`
- 在 Controller 中手动拼失败响应替代统一异常体系

## 4. 模块级规则

### 4.1 `vita-admin` / `vita-service` / `vita-common`

#### 管理端接口风格

管理端 CRUD 接口命名沿用当前仓库风格：

- `add`
- `update`
- `delete`
- `getDetail`
- `list`
- `page`

扩展接口也应延续同一命名风格，例如：

- `getAll`
- `getByUserId`
- `getOptionListByUserId`
- `checkRoleNameUnique`

#### 鉴权规则

`/system/*` 控制器默认落 `@SaCheckPermission("system:资源:动作")`

动作规范：

- 读接口：`view`
- 新增：`add`
- 更新：`update`
- 删除：`delete`
- 授权类接口：`grant`

#### Service 默认骨架

普通业务模块默认生成：

- `create`
- `update`
- `delete`
- `get`
- `list`
- `page`

如果存在唯一性校验、可操作性校验、关联授权等需求，再按现有 RBAC 写法扩展：

- `check*Unique`
- `check*Allowed`
- `getAll`
- `getOptionList*`
- `auth*`
- `cancelAuth*`

#### Mapper 默认骨架

优先生成：

- `selectList(buildQueryWrapper(...))`
- `selectPage(searchDto, buildQueryWrapper(...))`
- 单表投影方法，如 `selectRoleCodesByIds`
- 单表计数方法，如 `countBy*`

不要默认生成：

- 带 `join` 的 XML
- 为了简单而把跨表结果一次性查出来的 SQL

#### 文档与 SQL 联动

以下改动必须同步更新文档或 SQL：

- 认证链路
- RBAC 权限规则
- 数据库表结构
- 初始化数据
- 升级脚本

对应位置：

- `docs/system/*`
- `sql/init/*`
- `sql/upgrade/*`

### 4.2 `vita-aichat`

AI Chat 模块沿用分层体系，但目录按子域组织。当前主要子域包括：

- `chat`
- `aiMessage`
- `aiConversation`
- `aiConversationSummary`
- `aiChatRequest`
- `aiMessageStreamBatch`

AI 生成代码时应遵守：

- Controller 不直接写模型调用逻辑
- 模型请求构造、响应解析、SSE 输出、并发治理、状态流转优先沉到 `service/support/model/property`
- 会话状态、消息状态、流式状态的变更必须显式处理成功、失败、超时、取消、中断边界
- 新增 AI Chat 接口、模型接入、状态机变化前，先同步 `docs/aichat/ai-chat-design.md`

不要生成：

- Controller 里直接编排 Prompt 或直接调用第三方模型 SDK
- 忽略流式链路中的失败回写、资源释放、状态收敛
- 把并发控制、幂等控制散落在多个 Controller 中

### 4.3 `property` 规则

后续 AI 生成的配置类必须单独放在领域级 `property` 包，包路径跟随子域，末尾固定 `.property`。

示例：

- `com.vita.aichat.chat.property.AiChatProperty`
- `com.vita.xxx.yyy.property.XxxYyyProperty`

规则如下：

- 命名统一为 `XxxProperty`
- 不生成 `*Properties`
- 不把配置字段直接放进 `config`、`service`、`support`
- 一组相关配置项优先生成 `@ConfigurationProperties`
- 只有零散单值配置才允许 `@Value`
- starter 模块也遵守相同规则

说明：

- 当前仓库里存在 `@Value` 风格的历史实现，后续新生成代码应以 `@ConfigurationProperties + XxxProperty` 为优先方向
- 历史代码不要求本次顺手重构，但新代码不要继续放大不一致

### 4.4 `vita-starters`

starter 模块生成规则：

- 自动装配类只负责装配，不承担业务逻辑
- 配置项统一走独立 `property` 类
- `config` 负责 `@Bean`、装配条件、启用属性
- 复杂逻辑放在 starter 自己的 service / support 中

不要生成：

- 在自动装配类中直接堆积业务流程
- 配置字段散落到多个 Config 类里

### 4.5 `vita-generator`

生成器模板输出必须与主项目规则一致：

- 返回体仍是 `CommonResult` / `PageResponse`
- 查询仍用 `LambdaQueryWrapperX`
- 分页仍用 `PageRequest`
- Service 骨架仍遵守事务与异常规则
- DTO / VO 命名与继承关系必须一致

如果新增或修改模板，需要同步检查模板是否违反本文的硬规则。

## 5. 明确禁用项

以下写法在 AI 生成代码中视为禁用：

- `join` SQL
- Controller 中编排复杂业务
- Service 中完成单表投影、单表计数、单表排序这类 Mapper 细节
- 同类 `public @Transactional` 方法互调
- 通过互相注入 Service 规避事务或复用逻辑
- 生成另一套返回体或分页对象
- 新增 `*Properties` 命名
- 把 Property 类散落在 `config` / `service`
- 未经设计文档更新直接扩展 AI Chat 模型链路
- 直接输出敏感配置，如 API Key、Token、数据库密码

## 6. AI 生成前后检查清单

### 6.1 生成前

- 确认目标模块和依赖边界
- 确认是否属于管理端 CRUD、RBAC 扩展、AI Chat、starter、generator 模板
- 确认是否需要同步文档或 SQL
- 确认是否应生成 `Property` 类

### 6.2 生成后

- 是否仍遵守分层
- 是否使用了 `CommonResult` / `PageResponse`
- 是否使用了 `BaseMapperX` / `LambdaQueryWrapperX`
- 是否误用了 `join`
- 是否把单表细节下沉到了 Mapper
- 是否把事务边界放在外部入口
- 是否给 `/system/*` 落了正确的 `@SaCheckPermission`
- 是否需要同步 `docs/system/*`、`docs/aichat/*`、`sql/*`
- 是否需要补单元测试或关键链路回归测试

## 7. Codex Prompt Rules

以下文本可直接作为 `Codex` 的项目级规则输入：

```text
你正在 be-vita 仓库中生成代码。必须遵守以下规则：

1. 包前缀统一使用 com.vita.*。
2. 严格遵守分层：Controller 只接参和返回 CommonResult；业务逻辑只在 Service；持久化只在 Mapper。
3. 管理端接口命名统一使用 add/update/delete/getDetail/list/page，返回结构统一 CommonResult<T>，分页统一 PageRequest/PageResponse<T>。
4. SearchDto 默认继承 PageRequest；UpdateDto 默认继承 CreateDto 并补 id。
5. 查询优先 BaseMapperX + LambdaQueryWrapperX，新代码禁止退回原生 LambdaQueryWrapper。
6. 禁止写 join SQL。跨表查询统一由 Service 编排：先查关联 ID，去重后，再查目标表。
7. 单表投影、单表计数、单表排序属于 Mapper 细节，不要在 Service 中 selectList 后 stream().map() 组装。
8. 事务边界只放外部入口；同一类中的 public @Transactional 方法禁止互调，统一抽 private 共享逻辑。
9. /system/* 控制器统一落 @SaCheckPermission("system:资源:动作")，动作只允许 view/add/update/delete/grant。
10. 参数非法统一抛 ServiceException(GlobalErrorCode.BAD_REQUEST)，数据不存在统一抛 ServiceException(GlobalErrorCode.NOT_FOUND)。
11. Property 类必须放在领域级 property 包，命名统一 XxxProperty，不要生成 *Properties。对一组配置项优先使用 @ConfigurationProperties。
12. AI Chat 相关代码中，不要把模型编排写进 Controller；模型调用、流式处理、状态流转、并发治理优先沉到 service/support/model/property。
13. starter 自动装配类只做装配，不承载业务逻辑；starter 配置项统一走 property 类。
14. 生成器模板输出必须保持与主项目相同的分层、返回体、分页、Wrapper、事务规则。
15. 若改动认证、RBAC、SQL、AI Chat 设计或初始化数据，必须同步更新 docs/system/*、docs/aichat/*、sql/init/*、sql/upgrade/*。

明确禁止：
- join SQL
- Controller 组装复杂业务
- Service 处理单表投影或单表计数
- public @Transactional 同类互调
- 生成新的 Result/Page 类型
- 生成 *Properties 命名
- 把 Property 类散落到 config 或 service
```

## 8. Qwen Prompt Rules

以下文本可直接作为 `Qwen` 的项目级规则输入：

```text
你在 be-vita 项目中生成代码，请严格按现有工程风格输出，不要发明新的结构。

项目基线：
- 包前缀统一 com.vita.*
- 主链路是 vita-admin -> vita-service -> vita-common
- vita-aichat 独立于 vita-admin，但共享 common
- 返回体统一 CommonResult<T>
- 分页统一 PageRequest / PageResponse<T>

生成规则：
- Controller 仅负责接参、鉴权、调用 Service、返回结果，不写复杂业务。
- Service 负责业务编排、事务、跨表聚合、参数校验。
- Mapper 负责单表持久化和 SQL 细节。
- 管理端 CRUD 接口命名保持 add/update/delete/getDetail/list/page。
- /system/* 控制器统一使用 @SaCheckPermission("system:资源:动作")。
- SearchDto 默认继承 PageRequest，UpdateDto 默认继承 CreateDto 并增加 id。
- 查询优先使用 BaseMapperX 和 LambdaQueryWrapperX。
- 禁止 join SQL；有关联关系时，先查询关联 ID，去重后，再查询目标表内容。
- 单表投影、计数、排序属于 Mapper 细节，不要在 Service 里 selectList 后再自己 stream 组装。
- 写接口需要事务时，把 @Transactional 放在外部入口方法上；同类 public 事务方法不要互相调用。
- 参数非法时抛 ServiceException(GlobalErrorCode.BAD_REQUEST)，数据不存在时抛 ServiceException(GlobalErrorCode.NOT_FOUND)。

Property 规则：
- 配置类必须放在领域级 property 包，例如 com.vita.aichat.chat.property。
- 类名统一 XxxProperty，不要生成 *Properties。
- 一组配置项优先使用 @ConfigurationProperties，只有零散单值才使用 @Value。

AI Chat 规则：
- 不要在 Controller 里直接写模型调用或 Prompt 编排。
- 模型请求、流式处理、并发治理、状态回写优先沉到 service/support/model/property。
- 如果新增 AI Chat 接口、模型接入、会话或消息状态流转，需要先对齐 docs/aichat/ai-chat-design.md。

Starter 和 Generator 规则：
- starter 的 config 只做装配，配置项走 property 类，业务逻辑放 service/support。
- generator 模板输出必须继续遵守本项目的分层、返回体、分页、Wrapper、事务规则。

禁止输出：
- join SQL
- Controller 组装业务
- Service 处理单表投影和单表计数
- public @Transactional 同类互调
- 新的 Result/Page 类型
- *Properties 命名
- 把 Property 类散落到 config、service
```

## 9. 使用建议

- 如果只是生成一个普通管理端模块，优先使用“全项目共用硬规则 + `vita-admin/vita-service/vita-common` 规则”。
- 如果是 AI Chat 能力扩展，额外加载本文的 `vita-aichat` 规则，并同步阅读 `docs/aichat/ai-chat-design.md`。
- 如果涉及 starter 或生成器模板，必须显式检查 `property` 命名、装配边界和模板输出是否一致。
