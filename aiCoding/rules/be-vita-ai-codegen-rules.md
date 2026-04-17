# be-vita AI Coding Rules

## 1. 文档目标

本文档用于约束 `be-vita` 仓库中的 AI 代码生成行为，适用于 `Codex`、`Qwen` 以及其他基于 Prompt 的代码生成工具。

本文档不是通用编码规范，而是 `be-vita` 仓库的工程化生成规则。AI 在本仓库中生成、修改或重构代码时，必须优先遵守本文规则，避免生成结果偏离现有模块边界、命名习惯、分层方式和持久化约束。

本文档覆盖以下模块：

- `vita-admin`
- `vita-service`
- `vita-common`
- `vita-aichat`
- `vita-starters`
- `vita-generator`

本文档作为全项目 AI 代码生成的主规则源。仓库入口摘要可放在根目录 `AGENTS.md`，但详细规则以本文档为准。

---

## 2. 项目上下文

### 2.1 模块与依赖主链

当前仓库是 Maven 多模块后端项目，核心依赖链路保持如下：

```text
vita-admin -> vita-service -> vita-common
vita-aichat -> vita-common
```

其中：

- `vita-admin` 是管理端入口模块，主要承载 Controller、启动配置和管理端接口暴露。
- `vita-service` 是主业务服务模块，承载业务编排、领域服务、Mapper、DTO、VO 和实体定义。
- `vita-common` 是公共基础模块，承载通用工具、统一返回、异常、分页、Wrapper 扩展等能力。
- `vita-aichat` 是 AI Chat 独立业务模块，不依赖 `vita-admin`，仅共享公共基础能力。
- `vita-starters` 是通用 Starter 能力模块，用于封装公共自动装配、基础组件配置和可复用扩展能力。
- `vita-generator` 是代码生成器与模板模块，用于维护代码生成逻辑、模板文件和相关生成配置。

`vita-starters` 与 `vita-generator` 均通过 `pom.xml` 按需引入。若未被业务模块依赖，则在项目运行、Debug 调试以及 Maven 构建过程中不会参与执行，也不会影响主业务模块的启动和构建。

AI 生成代码时必须遵守以下依赖边界：

- `vita-admin` 只依赖 `vita-service` 和 `vita-common`。
- `vita-service` 不依赖 `vita-admin`。
- `vita-aichat` 不依赖 `vita-admin`。
- 业务代码不得跨层直接访问其他模块的内部实现。
- 不得为了临时复用逻辑破坏模块边界。

### 2.2 统一技术基线

AI 生成代码必须遵守以下技术基线：

- 包前缀统一使用 `com.vita.*`。
- Web 返回结构统一使用 `CommonResult<T>`。
- 分页请求统一使用 `PageRequest`。
- 分页响应统一使用 `PageResponse<T>`。
- MyBatis-Plus 查询优先使用 `BaseMapperX` 与 `LambdaQueryWrapperX`。
- RBAC 管理端接口统一使用 `@SaCheckPermission`。
- 写操作事务默认使用 `@Transactional(rollbackFor = Exception.class)`。
- 参数校验使用 `jakarta.validation`，不要使用旧版 `javax.validation`。

---

## 3. 全项目共用硬规则

### 3.1 分层规则

AI 生成代码时必须严格遵守 Controller、Service、Mapper、Entity 的职责边界。

#### Controller 职责

Controller 只负责：

- 接收请求参数。
- 执行权限注解声明。
- 调用 Service。
- 返回 `CommonResult`。

Controller 不负责：

- 复杂业务判断。
- 跨表数据组装。
- 直接调用 Mapper。
- 直接编排第三方服务调用。
- 手动拼接失败响应替代统一异常体系。

#### Service 职责

Service 负责：

- 业务编排。
- 参数校验。
- 事务控制。
- 跨表聚合。
- 状态流转。
- 操作合法性判断。

Service 不应承担：

- 单表 SQL 细节。
- 单表字段投影。
- 单表计数。
- 单表排序。
- 大段 SQL 字符串拼接。

#### Mapper 职责

Mapper 负责：

- 单表持久化。
- 单表查询条件封装。
- 单表投影。
- 单表计数。
- 单表排序。
- 必要的 XML / 手写 SQL。

#### Entity 职责

Entity 只表示数据库表结构和持久化字段，不承担 Controller 入参语义，也不要把 Web 请求校验规则直接写进 Entity。

禁止出现以下情况：

- Controller 直接调用 Mapper。
- Controller 中编写复杂业务判断或跨表组装。
- Service 中代替 Mapper 完成单表投影、计数、排序。
- 模块之间绕过 Service 直接访问内部实现。

---

### 3.2 命名规则

基础命名规则如下：

- 类名使用 `PascalCase`。
- 方法名、字段名使用 `camelCase`。
- 常量使用 `UPPER_SNAKE_CASE`。
- 包名全部小写，统一位于 `com.vita.*` 下。

常用类后缀必须保持一致：

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

配置类命名统一使用单数 `XxxProperty`，不要生成 `*Properties`。

---

### 3.3 返回体与分页规则

接口返回结构必须保持统一：

- 普通接口返回 `CommonResult<T>`。
- 分页接口返回 `CommonResult<PageResponse<*PageVo>>`。
- 列表接口返回 `CommonResult<List<*ListVo>>`。
- `SearchDto` 默认继承 `PageRequest`。
- `UpdateDto` 默认继承 `CreateDto` 并补充 `id` 字段。

禁止生成以下风格：

- 自定义另一套 `Result` / `Response` / `ApiResult`。
- 分页字段使用 `pageNo`、`size`、`records` 等与仓库不一致的命名。
- 在 Controller 中直接返回 MyBatis-Plus `Page`。
- 在 Service 中把 MyBatis-Plus `Page` 作为最终 Web 响应对象透传。

---

### 3.4 持久化与 SQL 规则

新代码查询优先使用：

```text
BaseMapperX + LambdaQueryWrapperX
```

Wrapper 条件优先使用：

- `eqIfPresent`
- `likeIfPresent`
- `inIfPresent`
- 其他仓库已有的 `*IfPresent` 扩展方法

#### 单表逻辑下沉规则

以下内容属于 Mapper 细节，应下沉到 Mapper：

- 单表字段投影。
- 单表排序。
- 单表去重。
- 单表计数。
- 单表条件封装。

禁止在 Service 中先 `selectList` 查询完整实体，再通过 `stream().map()` 完成本应由 Mapper 完成的单表字段投影。

#### 关联查询规则

新代码禁止生成 `join` SQL。

关联查询统一遵循以下方式：

```text
先查关联 ID -> 去重 -> 再查目标表 -> Service 中完成聚合组装
```

不要为了图省事生成一次性跨表查询 SQL，也不要在 XML 中偷偷补 `join`。

#### 逻辑删除规则

- 基于 MyBatis-Plus Wrapper 的普通查询，不重复手写 `is_deleted = 0`。
- 只有 XML / 手写 SQL 才需要显式补充逻辑删除过滤条件。

#### 嵌套类规则

禁止在所有类中继续嵌套多层 `static class`。

不推荐生成如下结构：

```java
@Data
public class AiChatProperty {

    @Data
    public static class Lock {

        private Conversation conversation = new Conversation();

        @Data
        public static class Conversation {
            private Long leaseSeconds = 600L;
        }
    }
}
```

建议将复杂配置拆成独立 Property 类，或使用扁平结构降低维护成本。

---

### 3.5 事务规则

事务边界只放在外部可调用入口方法上。

写接口如果需要事务，例如 `create`、`update`、`delete`、`batch`，直接在对外入口方法上标注：

```java
@Transactional(rollbackFor = Exception.class)
```

同一 Service 中多个 `public @Transactional` 方法禁止互相直接调用。共用逻辑统一提取为 `private` 方法。

禁止出现以下写法：

- `delete()` 直接调用同类中的 `deleteBatch()`。
- 为了事务生效引入 `self` 注入。
- 通过循环依赖规避事务失效问题。
- 默认依赖 `AopContext.currentProxy()` 触发事务。

---

### 3.6 参数校验与异常规则

基础入参校验放在 DTO 上，统一使用 `jakarta.validation`。

Service 层必须补充以下校验：

- 空值校验。
- 关键 ID 校验。
- 当前登录上下文校验。
- 数据存在性校验。
- 操作合法性校验。

异常规则如下：

- 参数非法统一抛 `ServiceException(GlobalErrorCode.BAD_REQUEST)`。
- 数据不存在统一抛 `ServiceException(GlobalErrorCode.NOT_FOUND)`。
- 不要返回 `null` 表示失败。
- 不要直接 `throw new RuntimeException("xxx")`。
- 不要在 Controller 中手动拼接失败响应。

---

## 4. 模块级规则

### 4.1 `vita-admin` / `vita-service` / `vita-common`

#### 4.1.1 管理端接口风格

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

不要生成 `createXxx`、`modifyXxx`、`removeXxx`、`queryPage` 等与仓库风格不一致的方法名。

#### 4.1.2 鉴权规则

`/system/*` 控制器默认使用：

```java
@SaCheckPermission("system:资源:动作")
```

动作命名规范如下：

- 读接口：`view`
- 新增：`add`
- 更新：`update`
- 删除：`delete`
- 授权类接口：`grant`

示例：

```java
@SaCheckPermission("system:role:view")
@SaCheckPermission("system:role:add")
@SaCheckPermission("system:role:update")
@SaCheckPermission("system:role:delete")
@SaCheckPermission("system:role:grant")
```

#### 4.1.3 Service 默认骨架

普通业务模块默认生成以下 Service 方法：

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

#### 4.1.4 Mapper 默认骨架

Mapper 优先生成以下方法：

- `selectList(buildQueryWrapper(...))`
- `selectPage(searchDto, buildQueryWrapper(...))`
- 单表投影方法，例如 `selectRoleCodesByIds`
- 单表计数方法，例如 `countBy*`

不要默认生成：

- 带 `join` 的 XML。
- 为了简单而一次性查出跨表结果的 SQL。
- 与 `BaseMapperX` 能力重复的无意义方法。

#### 4.1.5 文档与 SQL 联动

以下改动必须同步更新文档或 SQL：

- 认证链路。
- RBAC 权限规则。
- 数据库表结构。
- 初始化数据。
- 升级脚本。

对应位置：

- `docs/system/*`
- `sql/init/*`
- `sql/upgrade/*`

---

### 4.2 `vita-aichat`

`vita-aichat` 沿用全项目分层体系，但目录按子域组织。当前主要子域包括：

- `chat`
- `aiMessage`
- `aiConversation`
- `aiConversationSummary`
- `aiChatRequest`
- `aiMessageStreamBatch`

AI 生成 `vita-aichat` 相关代码时必须遵守：

- Controller 不直接写模型调用逻辑。
- Controller 不直接编排 Prompt。
- Controller 不直接调用第三方模型 SDK。
- 模型请求构造、响应解析、SSE 输出、并发治理、状态流转优先下沉到 `service` / `support` / `model` / `property`。
- 会话状态、消息状态、流式状态的变更必须显式处理成功、失败、超时、取消、中断等边界。
- 新增 AI Chat 接口、模型接入、状态机变化前，必须先同步 `docs/aichat/ai-chat-design.md`。

禁止生成以下内容：

- Controller 中直接拼接 Prompt。
- Controller 中直接调用 DashScope、Kimi、OpenAI、Dify 等模型 SDK 或 HTTP API。
- 忽略流式链路中的失败回写、资源释放、状态收敛。
- 将并发控制、幂等控制散落在多个 Controller 中。
- 在业务层强绑定某一个模型供应商，导致后续无法扩展 Provider。

---

### 4.3 `property` 规则

后续 AI 生成的配置类必须单独放在领域级 `property` 包中，包路径跟随子域，末尾固定为 `.property`。

示例：

```text
com.vita.aichat.chat.property.AiChatProperty
com.vita.xxx.yyy.property.XxxYyyProperty
```

规则如下：

- 命名统一为 `XxxProperty`。
- 不生成 `*Properties`。
- 不把配置字段直接放进 `config`、`service`、`support`。
- 一组相关配置项优先生成 `@ConfigurationProperties`。
- 只有零散单值配置才允许使用 `@Value`。
- starter 模块也必须遵守相同规则。

说明：

- 当前仓库中可能存在 `@Value` 风格的历史实现，后续新生成代码应以 `@ConfigurationProperties + XxxProperty` 为优先方向。
- 历史代码不要求在本次生成中顺手重构，但新代码不要继续放大不一致。

---

### 4.4 `vita-starters`

`vita-starters` 是通用 Starter 能力模块，主要承载自动装配和可复用基础能力。

AI 生成 starter 代码时必须遵守：

- 自动装配类只负责装配，不承担业务逻辑。
- 配置项统一走独立 `property` 类。
- `config` 只负责 `@Bean`、装配条件、启用属性和条件控制。
- 复杂逻辑放在 starter 自己的 `service` / `support` 中。
- 自动装配类应保持轻量，避免堆积流程代码。

禁止生成：

- 在自动装配类中直接编写业务流程。
- 配置字段散落到多个 Config 类中。
- starter 反向依赖具体业务模块。

---

### 4.5 `vita-generator`

`vita-generator` 是代码生成器与模板模块。模板输出必须与主项目工程规则保持一致。

生成器模板输出必须满足：

- 返回体仍使用 `CommonResult` / `PageResponse`。
- 查询仍使用 `LambdaQueryWrapperX`。
- 分页仍使用 `PageRequest`。
- Service 骨架仍遵守事务与异常规则。
- DTO / VO 命名与继承关系必须一致。
- Mapper 查询仍遵守单表下沉与禁止 `join` 的规则。

如果新增或修改模板，必须同步检查模板是否违反本文硬规则，避免生成器持续放大不一致代码。

---

## 5. 明确禁用项

以下写法在 AI 生成代码中视为禁用：

- 生成 `join` SQL。
- Controller 中编排复杂业务。
- Controller 中直接调用 Mapper。
- Controller 中直接调用第三方模型 SDK。
- Service 中完成单表投影、单表计数、单表排序等 Mapper 细节。
- 同类 `public @Transactional` 方法互相调用。
- 通过互相注入 Service 规避事务或复用逻辑。
- 生成另一套返回体或分页对象。
- 新增 `*Properties` 命名。
- 把 Property 类散落在 `config` / `service` / `support`。
- 未经设计文档更新直接扩展 AI Chat 模型链路。
- 直接输出敏感配置，例如 API Key、Token、数据库密码。
- 为了临时实现功能破坏模块依赖边界。

---

## 6. AI 生成前后检查清单

### 6.1 生成前检查

生成代码前，必须先确认：

- 目标模块属于 `vita-admin`、`vita-service`、`vita-common`、`vita-aichat`、`vita-starters` 还是 `vita-generator`。
- 是否会破坏模块依赖边界。
- 是否属于管理端 CRUD、RBAC 扩展、AI Chat、starter 或 generator 模板。
- 是否需要同步文档或 SQL。
- 是否应生成 `Property` 类。
- 是否涉及事务边界。
- 是否涉及权限码。
- 是否涉及 AI Chat 状态机或模型接入。

### 6.2 生成后检查

生成代码后，必须检查：

- 是否仍遵守 Controller / Service / Mapper 分层。
- 是否统一使用 `CommonResult` / `PageResponse`。
- 是否使用了 `BaseMapperX` / `LambdaQueryWrapperX`。
- 是否误用了 `join`。
- 是否把单表细节下沉到了 Mapper。
- 是否把事务边界放在外部入口方法。
- 是否存在同类 `public @Transactional` 方法互调。
- 是否给 `/system/*` 接口添加了正确的 `@SaCheckPermission`。
- 是否生成了错误的 `*Properties` 命名。
- 是否需要同步 `docs/system/*`、`docs/aichat/*`、`sql/init/*`、`sql/upgrade/*`。
- 是否需要补充单元测试或关键链路回归测试。

---

## 7. Codex Prompt Rules

以下文本可直接作为 `Codex` 的项目级规则输入：

```text
你正在 be-vita 仓库中生成代码。必须严格遵守以下规则：

1. 包前缀统一使用 com.vita.*。
2. 严格遵守分层：Controller 只负责接参、鉴权、调用 Service、返回 CommonResult；业务逻辑只在 Service；持久化只在 Mapper。
3. 管理端接口命名统一使用 add/update/delete/getDetail/list/page，返回结构统一 CommonResult<T>，分页统一 PageRequest/PageResponse<T>。
4. SearchDto 默认继承 PageRequest；UpdateDto 默认继承 CreateDto 并补 id。
5. 查询优先使用 BaseMapperX + LambdaQueryWrapperX，不要退回原生 LambdaQueryWrapper。
6. 禁止写 join SQL。跨表查询统一由 Service 编排：先查关联 ID，去重后，再查目标表。
7. 单表投影、单表计数、单表排序属于 Mapper 细节，不要在 Service 中 selectList 后 stream().map() 组装。
8. 事务边界只放外部入口方法；同一类中的 public @Transactional 方法禁止互调，共用逻辑统一抽 private 方法。
9. /system/* 控制器统一使用 @SaCheckPermission("system:资源:动作")，动作只允许 view/add/update/delete/grant。
10. 参数非法统一抛 ServiceException(GlobalErrorCode.BAD_REQUEST)，数据不存在统一抛 ServiceException(GlobalErrorCode.NOT_FOUND)。
11. Property 类必须放在领域级 property 包，命名统一 XxxProperty，不要生成 *Properties。一组配置项优先使用 @ConfigurationProperties。
12. AI Chat 相关代码中，不要把模型编排写进 Controller；模型调用、流式处理、状态流转、并发治理优先下沉到 service/support/model/property。
13. starter 自动装配类只做装配，不承载业务逻辑；starter 配置项统一走 property 类。
14. generator 模板输出必须保持与主项目相同的分层、返回体、分页、Wrapper、事务和异常规则。
15. 如果改动认证、RBAC、SQL、AI Chat 设计或初始化数据，必须同步更新 docs/system/*、docs/aichat/*、sql/init/*、sql/upgrade/*。

明确禁止：
- join SQL
- Controller 组装复杂业务
- Controller 直接调用 Mapper
- Controller 直接调用第三方模型 SDK
- Service 处理单表投影、单表计数、单表排序
- public @Transactional 同类互调
- 生成新的 Result/Page 类型
- 生成 *Properties 命名
- 把 Property 类散落到 config、service、support
- 直接输出敏感配置，如 API Key、Token、数据库密码
```

---

## 8. Qwen Prompt Rules

以下文本可直接作为 `Qwen` 的项目级规则输入：

```text
你在 be-vita 项目中生成代码，请严格按现有工程风格输出，不要发明新的结构。

项目基线：
- 包前缀统一 com.vita.*。
- 主链路是 vita-admin -> vita-service -> vita-common。
- vita-aichat 独立于 vita-admin，但共享 vita-common。
- 返回体统一 CommonResult<T>。
- 分页统一 PageRequest / PageResponse<T>。
- 查询优先 BaseMapperX + LambdaQueryWrapperX。

生成规则：
- Controller 仅负责接参、鉴权、调用 Service、返回结果，不写复杂业务。
- Service 负责业务编排、事务、跨表聚合、参数校验和状态流转。
- Mapper 负责单表持久化、单表查询条件、单表投影、单表计数和单表排序。
- 管理端 CRUD 接口命名保持 add/update/delete/getDetail/list/page。
- /system/* 控制器统一使用 @SaCheckPermission("system:资源:动作")。
- SearchDto 默认继承 PageRequest，UpdateDto 默认继承 CreateDto 并增加 id。
- 禁止 join SQL；有关联关系时，先查询关联 ID，去重后，再查询目标表内容。
- 单表投影、计数、排序属于 Mapper 细节，不要在 Service 里 selectList 后再 stream 组装。
- 写接口需要事务时，把 @Transactional(rollbackFor = Exception.class) 放在外部入口方法上；同类 public 事务方法不要互相调用。
- 参数非法时抛 ServiceException(GlobalErrorCode.BAD_REQUEST)，数据不存在时抛 ServiceException(GlobalErrorCode.NOT_FOUND)。

Property 规则：
- 配置类必须放在领域级 property 包，例如 com.vita.aichat.chat.property。
- 类名统一 XxxProperty，不要生成 *Properties。
- 一组配置项优先使用 @ConfigurationProperties，只有零散单值才使用 @Value。

AI Chat 规则：
- 不要在 Controller 里直接写模型调用或 Prompt 编排。
- 不要在 Controller 里直接调用第三方模型 SDK。
- 模型请求、流式处理、并发治理、状态回写优先下沉到 service/support/model/property。
- 如果新增 AI Chat 接口、模型接入、会话或消息状态流转，需要先对齐 docs/aichat/ai-chat-design.md。

Starter 和 Generator 规则：
- starter 的 config 只做装配，配置项走 property 类，业务逻辑放 service/support。
- generator 模板输出必须继续遵守本项目的分层、返回体、分页、Wrapper、事务和异常规则。

禁止输出：
- join SQL
- Controller 组装业务
- Controller 直接调用 Mapper
- Service 处理单表投影和单表计数
- public @Transactional 同类互调
- 新的 Result/Page 类型
- *Properties 命名
- 把 Property 类散落到 config、service、support
- 明文 API Key、Token、数据库密码
```

---

## 9. 使用建议

- 如果只是生成普通管理端模块，优先加载“全项目共用硬规则 + `vita-admin` / `vita-service` / `vita-common` 规则”。
- 如果是 RBAC 相关能力扩展，必须额外检查权限码、菜单权限、初始化 SQL 和 `docs/system/*`。
- 如果是 AI Chat 能力扩展，必须额外加载 `vita-aichat` 规则，并同步阅读 `docs/aichat/ai-chat-design.md`。
- 如果涉及 `vita-starters`，必须检查自动装配边界、`property` 命名和是否错误依赖业务模块。
- 如果涉及 `vita-generator`，必须检查模板输出是否继续遵守主项目规则，避免生成器批量放大错误代码。

---

## 10. 一句话原则

```text
Controller 保持薄，Service 做编排，Mapper 管单表；禁止 join，禁止乱命名，禁止破坏模块边界；AI Chat 不在 Controller 调模型，Starter 不在 AutoConfiguration 写业务，Generator 模板必须与主项目规则一致。
```
