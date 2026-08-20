# be-vita AI Coding Rules

## 1. 规则定位

本文档是 `be-vita` 仓库中 AI 生成、修改、重构代码时的最高优先级详细规则源，适用于 Codex、Claude、Qwen 及其他基于 Prompt 的代码生成工具。

- 根目录 `README.md` 只负责仓库总览与入口索引。
- 根目录 `CLAUDE.md`、`AGENTS.md` 只负责执行摘要。
- 详细规则只认 `AICoding/rules/be-vita-ai-codegen-rules.md`。

AI 生成结果如果与本文件冲突，以本文件为准。

## 2. 当前仓库事实

### 2.1 模块与依赖主链

当前仓库是 Maven 多模块后端项目，核心模块如下：

- `vita-common`
- `vita-service`
- `vita-workflow`
- `vita-admin`
- `vita-openapi`
- `vita-scheduler`
- `vita-generator`
- `vita-starters`

当前主依赖链保持：

```text
vita-admin    -> vita-service -> vita-common
vita-admin    -> vita-workflow -> vita-service -> vita-common
vita-openapi  -> vita-service -> vita-common
vita-scheduler -> vita-service -> vita-common
```

其中：

- `vita-admin` 是管理端启动模块，主要承载启动配置、系统业务控制器和具体流程业务控制器。
- `vita-openapi` 是公开查询启动模块，主要承载只读公开接口暴露。
- `vita-scheduler` 是调度触发启动模块，主要承载定时刷新或手工触发接口暴露。
- `vita-service` 是主业务服务层模块，承载系统业务、Mapper、DTO、VO、实体定义和服务实现。
- `vita-workflow` 是工作流基础设施模块，承载 Warm-Flow 配置、处理器、通用工作流 Controller
  契约及默认实现、资源文件和后续可独立演进的工作流基础能力。
- `vita-common` 是公共基础模块，承载统一返回、异常、分页、基础模型、MyBatis-Plus 扩展、Sa-Token、Redisson 和工具能力。
- `vita-generator` 是代码生成器与模板模块。
- `vita-starters` 是 profile 驱动的 starter 聚合父模块，当前包含 `vita-captcha-spring-boot-starter`、`vita-file-spring-boot-starter`、`vita-i18n-spring-boot-starter`、`vita-poi-spring-boot-starter`。

### 2.2 文档与脚本入口

- 文档总导航：`docs/README.md`
- 需求与验收文档：`docs/requirements/`
- 系统与 Starter 详细设计：`docs/architecture/`
- 开发、运维与本地工具文档：`docs/development/`
- 本地开发脚本：`tools/`
- CodeGraph 唯一脚本目录：`tools/codegraph/`
- 初始化脚本：`sql/init/`
- 升级脚本：`sql/upgrade/`
- AI 规则文档：`AICoding/rules/be-vita-ai-codegen-rules.md`

## 3. 必须遵守

### 3.1 模块边界

- 必须保持 `vita-admin -> vita-service -> vita-common`、`vita-admin -> vita-workflow -> vita-service -> vita-common`、`vita-openapi -> vita-service -> vita-common`、`vita-scheduler -> vita-service -> vita-common` 的依赖边界。
- 必须统一使用 `com.vita.*` 作为包前缀。
- 必须通过公开服务接口完成跨模块协作，不能直接跨模块访问内部 Mapper、内部实现类或配置细节。
- 必须把通用能力沉淀到 `vita-common` 或合适的 starter，不能为了临时复用破坏模块边界。

### 3.2 分层职责

- Controller 必须只负责接收参数、鉴权声明、调用 Service、返回 `CommonResult`。
- 通用流程定义、实例和任务 Controller 必须聚合在 `vita-workflow`；报销等具体流程业务 Controller
  必须保留在对应业务启动模块，不得下沉到工作流引擎适配层。
- Service 必须负责业务编排、参数校验、事务控制、跨表聚合、状态流转。
- Mapper 默认只负责单表持久化、单表条件封装、单表投影、单表计数、单表排序和必要的 XML。
- 仅 `vita-workflow` 允许在专用只读 Query Mapper 中使用 `join` 完成工作流定义、实例、任务、办理人和轨迹投影；引擎表状态写入仍必须调用 Warm-Flow 官方 Service，不能通过关联 SQL 或自建写 Mapper 修改。
- Entity 必须只表达数据库表结构，不承载 Controller 语义和 Web 校验规则。

### 3.3 统一技术基线

- Web 返回体必须使用 `CommonResult<T>`。
- 分页请求必须使用 `PageRequest`。
- 分页响应必须使用 `PageResponse<T>`。
- MyBatis-Plus 查询必须优先使用 `BaseMapperX` 与 `LambdaQueryWrapperX`。
- 参数校验必须使用 `jakarta.validation`。
- `/system/*` 管理端接口默认使用 `@SaCheckPermission("system:资源:动作")`。
- 写操作事务默认在外部入口方法上使用 `@Transactional(rollbackFor = Exception.class)`。

### 3.4 命名与目录

- 类名必须使用 `PascalCase`。
- 方法名、字段名必须使用 `camelCase`。
- 常量必须使用 `UPPER_SNAKE_CASE`。
- 包名必须全小写。
- 后缀必须保持一致：`*Controller`、`*Service`、`*ServiceImpl`、`*Mapper`、`*CreateDto`、`*UpdateDto`、`*SearchDto`、`*DetailVo`、`*ListVo`、`*PageVo`、`*OptionVo`、`*Property`。
- 配置类必须放在领域级 `property` 包中，命名必须使用单数 `XxxProperty`。

### 3.5 持久化与 SQL

- 单表查询必须优先使用 `BaseMapperX + LambdaQueryWrapperX`。
- 单表条件优先使用仓库已有的 `eqIfPresent`、`likeIfPresent`、`inIfPresent` 等扩展。
- 单表字段投影、单表计数、单表排序、单表去重必须下沉到 Mapper。
- 关联查询必须遵循“先查关联 ID -> 去重 -> 再查目标表 -> Service 聚合组装”。
- 只有 XML 或手写 SQL 时才显式补充 `is_deleted = 0`。

### 3.6 事务、校验与异常

- 同一 Service 中多个 `public @Transactional` 方法不得互相直接调用。
- 共用事务逻辑必须抽成 `private` 方法，而不是 `self` 注入或 `AopContext.currentProxy()`。
- Service 层必须补充空值、关键 ID、登录上下文、数据存在性和操作合法性校验。
- 参数非法必须抛 `ServiceException(GlobalErrorCode.BAD_REQUEST)`。
- 数据不存在必须抛 `ServiceException(GlobalErrorCode.NOT_FOUND)`。

### 3.7 文档联动

以下改动必须同步更新文档或 SQL：

- 认证链路、RBAC 权限规则、日志方案
- Starter 配置项、自动装配行为
- 数据库表结构、初始化脚本、升级脚本
- 生成器模板输出规范

对应位置：

- `docs/requirements/`
- `docs/architecture/`
- `docs/development/`
- `sql/init/`
- `sql/upgrade/`

### 3.8 标准 CRUD 生成约束

- 新增表结构后，如需要落 Entity、DTO、VO、Mapper、Service、Controller、Model 这类标准 CRUD 骨架，必须先参照 `vita-generator` 模板生成基础骨架。
- 允许在模板生成结果上做必要业务补充、裁剪和重构，但不得绕过模板从零随意手写一套风格不一致的 CRUD。
- 该约束适用于普通业务表、日志表、workflow 业务扩展表等仓库自建表。
- 纯关联表、第三方组件内置表，以及明确不需要标准 CRUD 的场景，不触发该约束。

### 3.9 核心业务代码中文注释

- 核心业务代码必须在关键位置补充中文注释。
- 注释重点包括业务规则、事务边界、状态流转、并发控制、外部系统集成假设、特殊校验和重要异常处理原因。
- 注释应解释原因、约束和边界，不应重复代码表面含义。
- 不要求给 getter/setter、直白的 CRUD 转发、模板骨架或可直接从命名读懂的简单语句逐行加注释。

### 3.10 CodeGraph 使用约束

- CodeGraph 是可选的开发期本地代码分析工具，不属于 Maven 依赖、应用运行时、部署组件或 CI 门禁。
- CodeGraph 固定使用 `1.5.0`，仓库级唯一命令入口必须是 `./tools/codegraph/codegraph.sh`，相关脚本只能位于 `tools/codegraph/`。
- 人工或代理执行安装、初始化、状态检查、同步、重建、查询和回滚时必须使用仓库级入口，不得直接调用 CodeGraph CLI；个人 MCP 内部执行官方 `codegraph serve --mcp` 是唯一例外。
- `setup` 与 `teardown --yes` 可以按文档显式修改个人 npm 和 MCP 配置，其他子命令不得修改个人环境。
- 当 CodeGraph 可用且仓库根目录已存在有效索引时，跨模块调用链、接口实现、影响范围和 MyBatis Java/XML 映射必须优先使用 `codegraph_explore` 辅助分析。
- 查询必须优先使用完整类名，或使用“类名 + 方法名”的精确符号组合，禁止仅依赖宽泛自然语言查询后直接下结论。
- 工作流 Port/Adapter、多级接口继承、多实现类、反射调用和框架运行时绑定必须再使用 `rg` 与源码读取复核。
- CodeGraph 无结果、结果含糊、索引过期或工具不可用时，必须立即回退到 `rg`、文件读取和 Maven 验证，不得阻塞开发任务。
- CodeGraph 输出只能作为代码理解依据之一；Java 编译、Maven 测试、SQL 检查和源码事实拥有最终优先级。
- 团队试点固定版本、个人 MCP 注册、维护与回滚流程以 `docs/development/codegraph-integration.md` 为准。

### 3.11 上下文与导航

- 开始实现或评估前，必须先检查目标模块的现有目录结构、技术栈、`git status --short --branch` 和局部约定，不得仅依据需求描述或外部经验假设实现方式。
- 提及既有需求、缺陷修复或历史改动时，必须先检索 `docs/` 中的相关文档、Git 提交记录和当前实现，再决定修改方案。
- 必须从 `docs/README.md` 进入中央文档；需求与验收放在 `docs/requirements/`，技术详设放在 `docs/architecture/`，开发和运维指南放在 `docs/development/`。
- 文档与实现冲突时，必须明确指出冲突并以当前源码、数据库脚本和可执行测试结果复核，不能静默选择任一方。

### 3.12 信息获取与输出

- 检索代码时必须先使用精确文件名、类名、方法名或关键字定位，再读取必要文件片段；不得默认读取大型文件、全量日志或全量差异。
- 命令输出必须设置合理上限；构建失败、SQL、Mapper XML、日志和 `git diff` 等长输出只保留与当前判断相关的片段和结论。
- 已在 `docs/requirements/`、`docs/architecture/` 或契约文件中固化的背景，不得在会话或新文档中重复粘贴；后续任务必须直接引用对应文件和章节。
- 使用 CodeGraph 时必须优先精确符号查询，并通过 `CODEGRAPH_OUTPUT_MAX_LINES` 或工具输出预算限制返回规模。

## 4. 应该遵守

- 应该为新业务逻辑、异常分支、参数校验和关键状态流转补测试。
- 应该在设计先行的场景中先更新文档，再落代码。
- 应该在修改 `vita-generator` 模板时同步检查模板输出是否继续满足本文件全部硬规则。
- 应该优先使用仓库现有骨架与模式，不自行引入第二套返回体、分页对象、异常语义或目录约定。
- 应该在 CodeGraph 分析前确认索引位于仓库根目录，并在分支切换或结果异常时检查索引状态。

## 5. 明确禁止

- 禁止 `vita-workflow` 之外的模块编写或生成 `join` SQL。
- 禁止代码生成器生成 `join` SQL；`vita-workflow` 的只读关联查询必须手工设计并保持在专用 Query Mapper 内。
- 禁止 Controller 直接调用 Mapper。
- 禁止 Controller 中编写复杂业务判断、跨表组装或第三方 SDK 调用。
- 禁止 Service 中用 `selectList().stream().map()` 替代本应由 Mapper 完成的单表投影。
- 禁止新增第二套 `Result` / `Response` / `ApiResult` / 分页对象。
- 禁止新增 `*Properties` 命名。
- 禁止把配置字段散落到 `config`、`service`、`support` 中。
- 禁止为了临时实现功能破坏模块边界。
- 禁止直接输出密码、Token、数据库密码、模型 API Key 等敏感信息。
- 禁止提交 `.codegraph/`、本地 SQLite 索引或个人 MCP 配置。
- 禁止仅依据 CodeGraph 推断未明确解析的动态调用关系并直接修改核心业务代码。
- 禁止把 Java、Mapper、SQL、配置资源或其他业务代码放到 `tools/`。
- 禁止把需求或详细设计文档散落到 `vita-*` Maven 子模块中。
- 禁止删除 `tools/codegraph/`、在其他目录新增 CodeGraph 脚本，或绕过仓库级 CodeGraph 机制。
- 禁止新增绕过 `./tools/codegraph/codegraph.sh` 的模块级别名、自定义启动脚本或其他入口。

## 6. 新建表默认字段 DDL 规范

### 6.1 适用范围

本规范适用于除纯关联表外的所有新表，包括：

- 业务主表
- 日志表
- 需要审计、逻辑删除、租户隔离或乐观锁控制的扩展表

纯关联表例外：

- 如 `sys_user_role`、`sys_role_menu` 这类纯中间关系表，可按场景精简字段。
- 纯关联表不强制带完整审计后缀，但必须在设计文档或 SQL 中说明原因。
- `Warm-Flow` 官方引擎表属于第三方组件内置表，可按官方 DDL 原样落库，不强制补齐本仓库默认公共字段，也不强制改写字段顺序。
- 基于 `be-vita` 业务自建的 workflow 扩展表、流程业务表、审批关联表，仍必须完整遵守本规范。

本规范不要求本次批量追溯改写全部历史表；历史表以渐进式方式对齐。

### 6.2 强制字段

除纯关联表外，新建表默认公共字段必须包含：

- `tenant_id`
- `create_time`
- `update_time`
- `is_deleted`
- `create_by_id`
- `create_by`
- `update_by_id`
- `update_by`
- `version`
- `remark`

### 6.3 字段顺序

新建表字段布局必须保持：

```text
id -> tenant_id -> 业务字段 -> 公共审计后缀
```

说明：

- `tenant_id` 固定放在 `id` 后。
- 业务字段放在中间。
- 审计后缀固定收口在尾部。
- 业务专属字段可以插在中间，例如 `sys_user.pwd_update_date`，但不得混入公共审计后缀内部。

### 6.4 默认值约束

默认值必须保持：

- `create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
- `is_deleted TINYINT(1) DEFAULT 0`
- `version BIGINT DEFAULT 0`
- `tenant_id` 默认 `NULL`
- `create_by_id`、`create_by`、`update_by_id`、`update_by` 默认 `NULL`
- `remark` 默认 `NULL`

### 6.5 代表性样例

`sys_user` 作为本仓库“业务主表 + 公共审计后缀”的代表性样例。推荐结构为：

```sql
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint                                                        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`       bigint                                                                 DEFAULT NULL COMMENT '租户ID',
    ...业务字段...,
    `pwd_update_date` datetime                                                               DEFAULT NULL COMMENT '密码最后更新时间',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`     datetime                                                               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)                                                              DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`    bigint                                                                 DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`    bigint                                                                 DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                                                                 DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息表';
```

## 7. 模块专项规则

### 7.1 `vita-admin` / `vita-service` / `vita-common`

- 管理端 CRUD 命名沿用仓库当前风格：`add`、`update`、`delete`、`getDetail`、`list`、`page`。
- 普通业务模块默认生成 `create`、`update`、`delete`、`get`、`list`、`page` 六类 Service 方法。
- 管理端扩展方法优先保持现有风格，例如 `getAll`、`getOptionListByUserId`、`check*Unique`、`auth*`、`cancelAuth*`。

### 7.2 `vita-openapi` / `vita-scheduler`

- 只读公开接口必须保持薄 Controller、薄启动模块，不得在启动模块中下沉复杂业务编排。
- 调度触发接口只负责触发、鉴权和返回结果，核心刷新逻辑必须下沉到 `vita-service`。

### 7.3 `vita-starters`

- 自动装配类只负责装配和条件控制，不承担业务逻辑。
- 配置项必须放到独立 `property` 类中。
- starter 不得反向依赖具体业务模块。

### 7.4 `vita-generator`

- 生成器模板输出必须继续使用 `CommonResult`、`PageRequest`、`PageResponse`、`BaseMapperX`、`LambdaQueryWrapperX`。
- 模板输出必须继续遵守分层规则、事务规则、异常规则和 DDL 规范。
- 新增表如需标准 CRUD，必须先以 `vita-generator` 模板输出为起点，再做业务改造，不得绕过模板直接手写一套。
- 修改模板时必须检查是否引入了新的目录约定、命名后缀或第二套返回体。

## 8. 交付检查清单

### 8.1 生成前

- 检查 `git status --short --branch`、目标目录结构、技术栈和局部约定。
- 涉及既有需求、缺陷或历史改动时，检查中央文档、提交记录和当前实现。
- 确认目标模块和依赖边界。
- 确认是否属于管理端、公开接口、调度端、starter 或 generator 模板场景。
- 确认是否需要同步 `docs/`、`sql/`。
- 确认新表是否落入“默认字段规范”适用范围。
- 涉及跨模块调用链或影响分析时，确认 CodeGraph 是否可用；不可用时直接使用仓库基础工具继续分析。

### 8.2 生成后

- 检查 Controller 是否保持轻量。
- 检查 Service 是否没有承担 Mapper 细节。
- 检查新增表对应的标准 CRUD 是否先以 `vita-generator` 模板为起点生成。
- 检查 `vita-workflow` 之外是否出现 `join SQL`，并检查第二套返回体、`*Properties`、直接 SDK 调用等禁用项。
- 检查 `vita-workflow` 中的 `join SQL` 是否仅存在于专用只读 Query Mapper，且没有引擎表写 SQL。
- 检查核心业务代码是否只在关键位置增加中文注释，而不是缺失注释或机械式逐行注释。
- 检查文档、初始化脚本、升级脚本是否同步。
- 检查新表 DDL 是否满足字段顺序、默认值和例外规则。
- 检查 CodeGraph 得出的关键调用关系是否已经过源码、编译或测试复核。
- 检查需求、详设和开发指南是否放入正确的中央文档目录，且不存在个人机器绝对路径。
- 检查 `tools/` 中是否只有本地开发工具，CodeGraph 脚本是否全部收口在 `tools/codegraph/`。

## 全局约束

### 不保留历史兼容逻辑

当前项目处于 pre-v1.0 阶段，暂无需要保护的外部消费者或稳定 API 契约。

默认不为历史实现保留兼容分支，不新增兼容开关、补丁参数、临时兜底逻辑或过渡适配层。

代码设计应优先遵循领域语义、职责正交和结构清晰原则。如果现有模型、接口或流程已经无法准确表达业务语义，应直接重构核心结构，而不是在旧逻辑上继续叠加判断、标识位或兼容分支。

除非任务明确要求，否则禁止引入 `legacy fallback`、`compat mode`、历史兼容适配器等实现。

### 代码实现优化

修改代码时，应优先保证实现简洁、语义清晰、职责明确。

需要重新梳理兜底逻辑、异常分支和边界处理，确保主业务流程清晰可读。删除死代码、无效分支、废弃参数、重复逻辑和不必要的抽象。

优先保留一条清晰的主实现路径，避免多个相似流程并存。如果存在重复逻辑，应抽取为命名清晰的私有方法、辅助组件或领域服务，但前提是能够提升可读性和职责边界。

只在关键位置增加中文注释，例如业务规则、并发控制、事务边界、状态流转、外部系统集成假设和重要异常处理原因。避免添加与代码含义重复的注释。

不要通过补丁式判断修复结构性问题。如果根本结构不合理，应直接重构模型、接口或流程。
