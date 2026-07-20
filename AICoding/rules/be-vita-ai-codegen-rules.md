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
vita-admin    -> vita-workflow -> vita-common
vita-openapi  -> vita-service -> vita-common
vita-scheduler -> vita-service -> vita-common
```

其中：

- `vita-admin` 是管理端启动模块，主要承载控制器、启动配置和管理端接口暴露。
- `vita-openapi` 是公开查询启动模块，主要承载只读公开接口暴露。
- `vita-scheduler` 是调度触发启动模块，主要承载定时刷新或手工触发接口暴露。
- `vita-service` 是主业务服务层模块，承载系统业务、Mapper、DTO、VO、实体定义和服务实现。
- `vita-workflow` 是工作流基础设施模块，承载 Warm-Flow 配置、处理器、资源文件和后续可独立演进的工作流基础能力。
- `vita-common` 是公共基础模块，承载统一返回、异常、分页、基础模型、MyBatis-Plus 扩展、Sa-Token、Redisson 和工具能力。
- `vita-generator` 是代码生成器与模板模块。
- `vita-starters` 是 profile 驱动的 starter 聚合父模块，当前包含 `vita-captcha-spring-boot-starter`、`vita-file-spring-boot-starter`、`vita-i18n-spring-boot-starter`、`vita-poi-spring-boot-starter`。

### 2.2 文档与脚本入口

- 系统设计文档：`docs/system/`
- Starter 设计文档：`docs/starter/`
- 初始化脚本：`sql/init/`
- 升级脚本：`sql/upgrade/`
- AI 规则文档：`AICoding/rules/be-vita-ai-codegen-rules.md`

## 3. 必须遵守

### 3.1 模块边界

- 必须保持 `vita-admin -> vita-service -> vita-common`、`vita-admin -> vita-workflow -> vita-common`、`vita-openapi -> vita-service -> vita-common`、`vita-scheduler -> vita-service -> vita-common` 的依赖边界。
- 必须统一使用 `com.vita.*` 作为包前缀。
- 必须通过公开服务接口完成跨模块协作，不能直接跨模块访问内部 Mapper、内部实现类或配置细节。
- 必须把通用能力沉淀到 `vita-common` 或合适的 starter，不能为了临时复用破坏模块边界。

### 3.2 分层职责

- Controller 必须只负责接收参数、鉴权声明、调用 Service、返回 `CommonResult`。
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

- `docs/system/`
- `docs/starter/`
- `sql/init/`
- `sql/upgrade/`

## 4. 应该遵守

- 应该为新业务逻辑、异常分支、参数校验和关键状态流转补测试。
- 应该在设计先行的场景中先更新文档，再落代码。
- 应该在修改 `vita-generator` 模板时同步检查模板输出是否继续满足本文件全部硬规则。
- 应该优先使用仓库现有骨架与模式，不自行引入第二套返回体、分页对象、异常语义或目录约定。

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
- 修改模板时必须检查是否引入了新的目录约定、命名后缀或第二套返回体。

## 8. 交付检查清单

### 8.1 生成前

- 确认目标模块和依赖边界。
- 确认是否属于管理端、公开接口、调度端、starter 或 generator 模板场景。
- 确认是否需要同步 `docs/`、`sql/`。
- 确认新表是否落入“默认字段规范”适用范围。

### 8.2 生成后

- 检查 Controller 是否保持轻量。
- 检查 Service 是否没有承担 Mapper 细节。
- 检查 `vita-workflow` 之外是否出现 `join SQL`，并检查第二套返回体、`*Properties`、直接 SDK 调用等禁用项。
- 检查 `vita-workflow` 中的 `join SQL` 是否仅存在于专用只读 Query Mapper，且没有引擎表写 SQL。
- 检查文档、初始化脚本、升级脚本是否同步。
- 检查新表 DDL 是否满足字段顺序、默认值和例外规则。

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

只在关键位置增加注释，例如业务规则、并发控制、事务边界、外部系统集成假设和重要异常处理原因。避免添加与代码含义重复的注释。

不要通过补丁式判断修复结构性问题。如果根本结构不合理，应直接重构模型、接口或流程。
