# 系统日志开发设计文档（轻量化版）

## 1. 背景

当前仓库已经具备基础日志能力：

- `vita-admin/src/main/resources/logback.xml` 已拆分 `sys-info.log`、`sys-error.log`、`sys-access.log`
- `application.yml` 已保留基础日志开关
- `vita-common/src/main/java/com/vita/aspect/LogAspect.java` 已预留 AOP 切点类
- 业务代码已默认使用 `@Slf4j` / `LoggerFactory` 输出运行日志

但从仓库现状和原始设计稿来看，日志体系仍处于“框架已起、规则未定、能力未闭环”的状态，主要问题如下：

1. `requestId` 只出现在日志 pattern 中，当前没有请求入口注入逻辑，链路追踪无法真正生效。
2. `sys-info.log` 使用 `ThresholdFilter(INFO)`，会同时接收 `WARN` 和 `ERROR`，导致错误日志在 `sys-info.log` 与 `sys-error.log` 中重复落盘。
3. `sys-access.log` 已配置专用 appender，但没有统一的 `@Log` 注解、参数过滤器、落库模型和异步持久化链路。
4. `LogAspect` 为空实现，说明控制器审计日志尚未真正落地。
5. 缺少统一的脱敏、超长字段截断、文件上传参数过滤和异常摘要策略，存在敏感信息泄露与日志膨胀风险。
6. 缺少配置模型、失败隔离、轻量化边界和验收标准，后续实现容易出现口径不一致。

本文档在原始需求基础上补齐日志分层、配置模型、技术实现、现有表使用约束和实施步骤，作为后续开发与评审的统一依据。

## 2. 设计目标

### 2.1 核心目标

- 以轻量化日志为核心目标，默认只记录必要摘要，不记录完整大对象，不做重复落盘。
- 基于现有 `logback.xml` 风格，构建统一的系统日志体系。
- 保持业务日志继续由 Service 层 `@Slf4j` 输出，不侵入业务实现。
- Controller 层通过自定义 `@Log` 注解记录审计日志，并支持异步持久化。
- 对敏感字段、超长文本、文件流、二进制内容进行统一过滤。
- 为每次请求注入 `requestId`，实现跨层级链路追踪。
- 保证日志“能查问题、能看行为、能控风险、能承受高并发”。

### 2.2 非目标

- 不在数据库中完整存储异常堆栈，完整堆栈只保留在文件错误日志中。
- 不把所有 Service 调用自动审计化，审计范围仅覆盖 Controller 操作入口。
- 不修改现有 `sys_oper_log` 表结构，本次仅规范写入字段、写入时机和写入内容。
- 不引入独立日志平台依赖，先在当前仓库内完成本地文件 + DB 的统一方案。

## 3. 日志分层设计

| 日志类型 | 目标 | 输出来源 | 输出位置 | 保留建议 |
| --- | --- | --- | --- | --- |
| 系统运行日志 | 记录启动、业务流程、任务执行、普通告警 | `@Slf4j`、框架日志 | `sys-info.log` | 30 天 |
| 错误异常日志 | 记录异常摘要、堆栈、系统故障 | 全局异常处理、Service 异常 | `sys-error.log` | 60 天 |
| 访问审计日志 | 记录接口调用、操作行为、耗时、用户、请求摘要 | `@Log` + AOP | `sys-access.log` + `sys_oper_log` | 文件 30 天，DB 按现有清理策略 |
| 安全过滤日志 | 对敏感字段、超长对象进行治理 | 日志工具层 | 作用于所有日志通道 | 与主日志一致 |

### 3.1 系统运行日志

- 使用场景：业务正常流程、定时任务、消息消费、监听器执行、启动信息。
- 输出原则：只记录关键业务节点，不打印完整大对象。
- 推荐级别：
  - `INFO`：核心流程、关键状态变化
  - `WARN`：可恢复异常、重试、参数边界
  - `ERROR`：不可恢复异常、系统故障

### 3.2 错误异常日志

- 统一由全局异常处理器、切面异常捕获、核心组件异常输出。
- 完整堆栈仅写入 `sys-error.log`。
- 数据库审计表只保存现有字段可承载的异常摘要、错误状态和请求上下文，不保存完整堆栈。

### 3.3 访问审计日志

- 记录 Controller 方法的访问与操作行为。
- 审计文件日志至少包含：模块、业务类型、请求地址、请求方式、操作人、IP、耗时、请求参数摘要、执行状态、`requestId`。
- 审计数据库仅写入现有 `sys_oper_log` 表可承载的摘要字段，默认不保存完整响应体。
- 审计日志文件用于快速排查，数据库用于后台查询、统计、审计留痕。

### 3.4 安全过滤日志

统一过滤以下内容：

- 明文密码、旧密码、新密码
- `token`、`Authorization`、Cookie、JWT
- 身份证号、手机号、邮箱等个人敏感信息
- 超长 JSON、Base64 内容、二进制流、文件上传对象
- 大集合、大分页结果、导出字节流

## 4. 模块职责划分

| 模块 | 职责 |
| --- | --- |
| `vita-admin` | 维护 `logback.xml`、Web 请求入口 `requestId` 注入、环境配置 |
| `vita-common` | 提供 `@Log` 注解、`LogAspect`、统一 `LogUtils`、日志配置对象、异步事件模型 |
| `vita-service` | 按业务节点输出运行日志，不直接负责审计落库 |
| `sql/` | 复用现有 `sys_oper_log` 表脚本，本次不新增表结构变更 |

建议新增或完善的代码位置如下：

- `vita-common/src/main/java/com/vita/annotation/Log.java`
- `vita-common/src/main/java/com/vita/aspect/LogAspect.java`
- `vita-common/src/main/java/com/vita/filter/RequestIdFilter.java`
- `vita-common/src/main/java/com/vita/property/LogProperties.java`
- `vita-common/src/main/java/com/vita/utils/LogUtils.java`
- `vita-service` 或具体业务模块中的 `SysOperLogService`

补充依赖：

- `vita-common/pom.xml` 建议增加 `spring-boot-starter-aop`

## 5. 统一日志规范

### 5.1 日志格式

建议沿用现有风格，并补充线程、类名、方法名、链路 ID：

```text
【级别】时间 [线程] 类名 - [方法,行号] - requestId - 日志内容
```

推荐 pattern：

```xml
<property name="LOG_PATTERN"
          value="【%level】%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %logger{50} - [%method,%line] - %X{requestId:-N/A} - %msg%n"/>
```

### 5.2 业务日志书写规范

- 业务层继续使用 `@Slf4j`。
- 使用占位符输出，不使用字符串拼接。
- 以摘要输出为默认策略，不直接输出完整 DTO、分页结果、文件流、二进制数组。
- 对外部接口调用、任务调度、状态变更、关键参数校验失败要保留明确日志。

示例：

```java
void main() {
  log.info("创建订单成功，orderId={}, userId={}", orderId, userId);
  log.warn("短信发送失败，mobile={}, retry={}", maskedMobile, retryCount);
  log.error("支付回调处理异常，orderId={}", orderId, e);
}
```

## 6. Logback 优化方案

### 6.1 目标

- 保持当前三类文件拆分。
- 避免 `ERROR` 在 `sys-info.log` 中重复输出。
- 保留审计日志专用 logger。
- 减少无效日志体积，避免同一请求写入过多重复内容。
- 为高并发场景预留异步 appender。

### 6.2 推荐配置调整

1. 将项目包 logger 从 `com.xxx.project` 改为 `com.vita`。
2. `sys-info.log` 仅接收 `INFO` 和 `WARN`，显式拒绝 `ERROR`。
3. `sys-access.log` 继续使用专用 logger 名称 `sys-access`。
4. 根 logger 保持 `INFO` 级别，`org.springframework` 默认 `WARN`。
5. 若接口量较大，建议在文件 appender 外包一层 `AsyncAppender`。

推荐示例：

```xml
<logger name="com.vita" level="INFO"/>
<logger name="org.springframework" level="WARN"/>
<logger name="sys-access" level="INFO" additivity="false">
    <appender-ref ref="FILE_ACCESS"/>
</logger>

<appender name="FILE_INFO" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_HOME}/sys-info.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG_HOME}/sys-info.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>128MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>${LOG_PATTERN}</pattern>
        <charset>UTF-8</charset>
    </encoder>
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>INFO</level>
    </filter>
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <level>ERROR</level>
        <onMatch>DENY</onMatch>
        <onMismatch>NEUTRAL</onMismatch>
    </filter>
</appender>
```

### 6.3 是否切换为 `logback-spring.xml`

当前项目已使用 `logback.xml`，如果只需要静态滚动配置，可以继续保持现状。

仅当需要以下能力时，建议切换为 `logback-spring.xml`：

- 根据 `spring.profiles.active` 切换不同日志目录
- 引用 Spring Environment 配置项
- 更细粒度地管理开发、测试、生产环境差异

## 7. requestId 链路设计

### 7.1 设计原则

- 每个 HTTP 请求必须有唯一 `requestId`。
- 优先复用请求头 `X-Request-Id`，前端未传时由后端生成。
- 在请求开始时写入 MDC，请求结束后清理。
- 同时回写响应头，便于前后端联调与问题定位。

### 7.2 建议实现

建议新增 `RequestIdFilter`：

1. 从请求头读取 `X-Request-Id`
2. 若为空，则生成 UUID 简化串
3. `MDC.put("requestId", requestId)`
4. `response.setHeader("X-Request-Id", requestId)`
5. `finally` 中执行 `MDC.remove("requestId")`

注意事项：

- 异步线程默认不会自动继承 MDC，不要依赖异步线程再从 MDC 读取审计信息。
- 审计落库事件对象应显式携带 `requestId`、用户信息和请求摘要。

## 8. 脱敏与大字段治理

### 8.1 脱敏目标

- 防止敏感信息进入文件日志和数据库日志。
- 在保证排障可用性的前提下，尽量保留字段结构。

### 8.2 脱敏规则

按“字段名关键字 + 数据特征”双维度处理：

| 类型 | 规则 |
| --- | --- |
| 密码 | 字段名匹配 `password`、`oldPassword`、`newPassword`，固定替换为 `******` |
| Token | 匹配 `token`、`authorization`、`accessToken`、`refreshToken`，显示前 6 后 4 |
| 手机号 | 保留前 3 后 4 |
| 身份证号 | 保留前 6 后 4 |
| 邮箱 | 保留首字符与域名 |
| Cookie / Header | 敏感键整体遮盖 |

### 8.3 截断规则

建议默认阈值：

- 单个字符串最大长度：`2000`
- 单次 JSON 序列化最大长度：`4096`
- 集合最大元素数：`50`
- Map 最大键值对数：`50`

超过阈值时输出摘要，例如：

```text
{"fileContent":"...(truncated,size=18342)"}
```

### 8.4 直接忽略的参数类型

- `MultipartFile`
- `HttpServletRequest`
- `HttpServletResponse`
- `BindingResult`
- `InputStream`
- `OutputStream`
- `File`
- `byte[]`

### 8.5 工具层建议

按当前轻量化目标，建议统一收敛到一个工具类：

- `LogUtils`：统一负责字段脱敏、超长内容截断、忽略类型过滤、对象摘要序列化

建议对外只暴露少量稳定方法，例如：

```java
  String LogUtils.toLogString(Object value);
  String LogUtils.toRequestLog(Object[] args, String[] excludeFields);
  String LogUtils.toResponseLog(Object result);

```

## 9. Controller 审计日志设计

### 9.1 注解设计

建议新增 `@Log` 注解，最少包含以下字段：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    String module();

    BusinessTypeEnum businessType() default BusinessTypeEnum.OTHER;

    boolean saveRequestData() default true;

    boolean saveResponseData() default false;

    boolean persist() default true;

    String[] excludeFields() default {};
}
```

推荐业务类型枚举：

- `QUERY`
- `CREATE`
- `UPDATE`
- `DELETE`
- `IMPORT`
- `EXPORT`
- `LOGIN`
- `LOGOUT`
- `OTHER`

### 9.2 切面执行流程

`LogAspect` 建议采用 `@Around + try/catch/finally` 实现：

1. 记录开始时间
2. 读取请求上下文、`requestId`、IP、URL、HTTP 方法
3. 读取当前登录用户 `LoginUserInfoModelContext`
4. 过滤并序列化请求参数
5. 执行目标方法
6. 过滤并序列化返回结果
7. 捕获异常并记录失败状态、异常摘要
8. 输出审计文件日志
9. 根据注解开关异步写入数据库

### 9.3 审计日志输出策略

- 成功操作：
  - 记录模块、业务类型、操作人、URL、耗时、请求摘要
- 失败操作：
  - 增加异常类名、错误摘要、失败状态
- 默认不记录完整响应体，只有确实需要审计返回结果的接口才开启 `saveResponseData`
- 登录、导出、导入等高风险操作：
  - 默认开启落库
- 高频查询接口：
  - 可只写文件，不落库，避免审计表快速膨胀

### 9.4 审计 logger 规范

切面统一使用：

```java
private static final Logger ACCESS_LOG = LoggerFactory.getLogger("sys-access");
```

这样可以与 `logback.xml` 中的专用 appender 保持一致。

## 10. 审计日志落库设计

### 10.1 落库原则

- 复用现有 `sys_oper_log` 表，不做表结构变更。
- 采用异步落库，避免阻塞主业务线程。
- 文件日志优先，数据库失败不能影响接口主流程。
- 数据库存储的是“查询友好型摘要信息”，不是完整调试日志镜像。

### 10.2 推荐异步方案

优先级从高到低：

1. Spring 事件 + `@Async`
2. 自定义 `ThreadPoolTaskExecutor`
3. 简单线程池包装服务

推荐做法：

- Controller 主线程只构造 `OperLogEvent`
- 事件对象中携带所有必要字段
- 异步消费者完成 DB 持久化
- 异步失败时仅写 `sys-error.log`，不向上抛出

### 10.3 现有表复用原则

本次直接复用现有 `sys_oper_log` 表，字段映射按现有结构处理，不新增字段、不调整索引、不修改默认值。

推荐写入口径：

- `module`：注解中的业务模块
- `business_type`：注解中的业务类型
- `request_method`：HTTP 方法
- `oper_url`：请求 URL
- `oper_ip`：客户端 IP
- `oper_location`：按现有能力可选填充，没有则置空
- `oper_name` / `oper_user_id`：当前登录用户
- `class_name` / `method_name`：目标类和方法
- `request_param`：经过 `LogUtils` 处理后的请求摘要
- `response_result`：仅在 `saveResponseData=true` 时写入摘要
- `status`：按现有约定写入成功/失败状态
- `error_msg`：异常摘要，禁止写完整堆栈
- `cost_time`：执行耗时
- `oper_time`：操作时间

### 10.4 写入边界说明

- `requestId` 继续放在 MDC 和文件日志中，用于串联链路，不强依赖数据库表字段承载。
- `request_param` 和 `response_result` 只保存摘要文本，不保存完整 JSON、大集合和二进制内容。
- 高频查询接口可关闭落库，仅保留文件审计日志。
- 数据库落库失败只记错误日志，不影响接口主流程。

## 11. 配置模型设计

建议统一新增 `vita.log` 配置前缀，不与 `sa-token.is-log` 混用。

示例：

```yaml
vita:
  log:
    enabled: true
    lightweight-mode: true
    request-id-header: X-Request-Id
    access-logger-name: sys-access
    mask:
      enabled: true
      keywords:
        - password
        - oldPassword
        - newPassword
        - token
        - authorization
        - idCard
        - mobile
    truncate:
      enabled: true
      max-string-length: 2000
      max-json-length: 4096
      max-collection-size: 50
    oper:
      enabled: true
      persist-enabled: true
      save-request-data: true
      save-response-data: false
      exclude-paths:
        - /auth/login
        - /captcha/**
    async:
      core-pool-size: 2
      max-pool-size: 4
      queue-capacity: 1000
```

建议使用 `@ConfigurationProperties(prefix = "vita.log")` 管理，而不是将日志相关属性零散写在多个 `@Value` 字段中。

## 12. 落地步骤

### 12.1 第一阶段：基础能力

1. 修正 `logback.xml` 中项目包名和 `FILE_INFO` 过滤规则
2. 新增 `RequestIdFilter`
3. 新增 `LogProperties`
4. 引入 `spring-boot-starter-aop`

### 12.2 第二阶段：日志治理

1. 实现统一 `LogUtils`
2. 集成脱敏、截断和忽略类型过滤
3. 补充参数过滤白名单和忽略类型

### 12.3 第三阶段：审计闭环

1. 定义 `@Log` 注解和业务类型枚举
2. 完成 `LogAspect`
3. 输出 `sys-access.log`
4. 实现异步持久化服务
5. 复用现有 `sys_oper_log` 表完成落库

### 12.4 第四阶段：验证与上线

1. 补充单元测试和集成测试
2. 进行大对象、敏感字段、异常场景压测
3. 验证高并发下文件日志与 DB 日志不会拖慢主链路

## 13. 测试与验收标准

### 13.1 必测场景

- 普通请求能生成 `requestId`
- 请求头传入 `X-Request-Id` 时能够透传
- `@Log` 标注接口能输出访问审计日志
- 敏感字段被脱敏
- 超长 JSON 被截断
- 文件上传接口不会打印文件内容
- 接口异常时同时生成错误日志和失败审计日志
- 数据库不可用时主接口仍正常返回，错误仅写入 `sys-error.log`

### 13.2 验收标准

| 验收项 | 标准 |
| --- | --- |
| 链路追踪 | 任意一次请求可通过 `requestId` 串联控制器、业务、异常和审计日志 |
| 安全治理 | 密码、Token、身份证号、手机号等敏感数据不会明文出现在日志 |
| 性能 | 审计落库失败不影响主流程，正常请求额外开销可控 |
| 可运维性 | 文件日志滚动、保留周期明确，现有表可直接复用 |
| 可扩展性 | 新增模块只需加注解或调用统一工具即可接入 |

## 14. 最终建议

结合当前仓库状态，建议按以下原则实施：

1. 不推翻现有 `logback.xml`，在现有结构上做增量优化。
2. 把公共能力全部沉淀到 `vita-common`，并统一收敛到一个 `LogUtils`，避免每个模块重复造轮子。
3. 审计日志采用“文件必写、数据库可选、摘要优先、失败不阻塞”的轻量化策略。
4. 复用现有 `sys_oper_log` 表，避免为了日志体系引入额外数据迁移成本。
5. 先完成 `requestId`、`LogUtils`、AOP 审计三件核心能力，再考虑更复杂的日志平台对接。

这样可以在最小改动成本下，快速把项目从“有日志配置”提升到“有统一日志体系”。
