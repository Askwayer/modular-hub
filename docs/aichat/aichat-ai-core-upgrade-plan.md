# AIChat 模块升级方案：ai-core starter 策略化改造

## 1. 升级背景

当前 `vita-aichat` 已经承载了 AI Chat 的完整业务流程，包括：

```text
1. 会话校验
2. 用户消息落库
3. 助手占位消息创建
4. 请求幂等控制
5. Redis 并发治理
6. SSE 流式返回
7. 流式批次落库
8. 最终消息状态回写
9. 异常处理
```

现阶段 `DashScope` 已经是正式业务链路，后续还需要逐步接入 `Kimi`，并预留 `FastAPI AI 服务` 的 demo 接入能力。

如果继续在 `vita-aichat` 业务代码中直接区分不同模型供应商，会出现两个问题：

```text
1. aichat 业务模块与具体 Provider SDK 强耦合
2. 新增一个 Provider 就需要修改主业务流程
```

因此，本次升级不再采用“Scene Slot / Capability Slot / Provider Slot”这类插槽细分方案，而是统一调整为：

> `vita-ai-core-spring-boot-starter` 通过策略模式处理不同业务模块，通过 Provider 路由分发到具体模型实现。

升级后的调用主链路固定为：

```text
vita-aichat
   ↓
AiChatClient
   ↓
AiModuleStrategy
   ↓
AiProviderRouter
   ↓
AiProvider
   ├── DashScope Provider
   ├── Kimi Provider
   └── FastAPI Demo Provider
```

---

## 2. 升级目标

本次升级目标如下：

```text
1. vita-aichat 只负责业务编排，不直接依赖具体模型 SDK
2. vita-ai-core-spring-boot-starter 提供统一协议、策略入口、Provider 路由和自动装配
3. 不再细分 Scene Slot / Capability Slot，而是按业务模块使用策略模式扩展
4. 所有策略和 Provider 都不耦合数据库，不依赖 Mapper、Entity、Repository、配置表
5. 运行时配置统一通过 application.yml 管理
6. DashScope 作为正式业务 Provider 基线保留现有语义
7. Kimi 作为正式业务 Provider 接入
8. FastAPI AI 服务只作为 demo Provider
9. 保持同步 chat 和流式 stream-chat 的统一抽象
10. 兼容 Spring Boot 3 starter 自动装配
```

---

## 3. 推荐最终模块结构

推荐按 `vita-starters` 聚合结构组织 AI 能力相关模块：

```text
be-vita
├── vita-aichat
│   └── AI Chat 业务编排、会话、消息、SSE、落库
│
├── vita-starters
│   ├── vita-ai-core-spring-boot-starter
│   │   ├── 统一请求/响应协议
│   │   ├── AiChatClient
│   │   ├── AiModuleStrategy
│   │   ├── AiModuleStrategyRegistry
│   │   ├── AiProviderRouter
│   │   ├── AiProvider
│   │   ├── 异常体系
│   │   └── 自动装配
│   │
│   ├── vita-ai-provider-dashscope-spring-boot-starter
│   │   └── DashScope 正式业务 Provider
│   │
│   ├── vita-ai-provider-kimi-spring-boot-starter
│   │   └── Kimi 正式业务 Provider
│   │
│   └── vita-ai-provider-fastapi-demo-spring-boot-starter
│       └── FastAPI demo Provider
│
└── vita-common
    └── 公共异常、上下文、基础工具
```

这里需要强调两点：

```text
1. Provider 模块是 vita-starters 下的并列 sibling 模块
2. Provider 模块依赖 ai-core，但不嵌套在 ai-core 模块内部
```

---

## 4. 核心分层职责

### 4.1 vita-aichat：业务编排层

`vita-aichat` 只负责 AI Chat 业务本身：

```text
1. 登录用户校验
2. 会话状态校验
3. requestId 幂等校验
4. Redis 锁与并发治理
5. 用户消息落库
6. 助手占位消息创建
7. 上下文消息组装
8. 调用 AiChatClient
9. 同步结果回写
10. 流式批次落库
11. SSE 推送
12. 异常处理与状态回写
```

不再承担：

```text
1. 不直接调用 DashScope SDK
2. 不直接调用 Kimi HTTP API
3. 不直接持有 Spring AI Prompt、ChatModel 等 Provider 细节
4. 不直接解析不同 Provider 的原始响应结构
5. 不在业务层写 if/else 判断不同模型供应商
```

### 4.2 vita-ai-core-spring-boot-starter：策略中心

`vita-ai-core-spring-boot-starter` 是 AI 调用抽象的统一入口，职责固定为：

```text
1. 定义统一请求与响应模型
2. 提供 AiChatClient
3. 定义 AiModuleStrategy
4. 提供 AiModuleStrategyRegistry
5. 提供 AiProviderRouter
6. 定义 AiProvider 标准接口
7. 统一异常体系
8. 提供 Spring Boot 3 自动装配
```

这里的设计重点不是“插槽”，而是“策略”：

```text
1. 不同业务模块由不同 AiModuleStrategy 处理
2. 当前唯一完整落地模块是 aichat
3. 后续新增业务模块时，只新增新的模块策略，不继续拆 scene/capability 子层
```

### 4.3 Provider 模块：模型适配层

每个 Provider starter 只负责一种具体模型实现：

```text
vita-ai-provider-dashscope-spring-boot-starter
  负责 DashScope 正式业务接入

vita-ai-provider-kimi-spring-boot-starter
  负责 Kimi 正式业务接入

vita-ai-provider-fastapi-demo-spring-boot-starter
  负责 FastAPI demo 接入
```

Provider 模块的边界固定为：

```text
1. 接收统一 AiChatRequest
2. 转换为各自 Provider 的请求结构
3. 发起同步或流式调用
4. 转换为统一 AiChatResponse / AiStreamChunk
5. 抛出 ai-core 统一异常
```

Provider 不负责：

```text
1. 不做会话落库
2. 不做业务鉴权
3. 不做 Redis 并发控制
4. 不依赖数据库配置表
```

---

## 5. 最终业务调用路径

### 5.1 同步对话调用路径

```text
前端
 ↓
AiChatController.send()
 ↓
AiChatServiceImpl.send()
 ↓
prepareSendContext()
 ↓
buildAiChatRequest()
 ↓
AiChatClient.chat()
 ↓
AiModuleStrategyRegistry.get("aichat")
 ↓
AichatModuleStrategy.prepare()
 ↓
AiProviderRouter.route()
 ↓
具体 AiProvider.chat()
 ↓
AiChatResponse
 ↓
回写 assistantMessage
 ↓
返回前端
```

### 5.2 流式对话调用路径

```text
前端 SSE 请求
 ↓
AiChatController.streamSend()
 ↓
AiChatServiceImpl.streamSend()
 ↓
prepareSendContext()
 ↓
buildAiChatRequest()
 ↓
AiChatClient.streamChat()
 ↓
AiModuleStrategyRegistry.get("aichat")
 ↓
AichatModuleStrategy.prepare()
 ↓
AiProviderRouter.route()
 ↓
具体 AiProvider.streamChat()
 ↓
Flux<AiStreamChunk>
 ↓
边推送 SSE，边聚合批次
 ↓
流结束后回写 assistantMessage
 ↓
释放请求锁与并发资源
```

---

## 6. ai-core 核心接口设计

### 6.1 AiChatClient

```java
public interface AiChatClient {

    AiChatResponse chat(AiChatRequest request);

    Flux<AiStreamChunk> streamChat(AiChatRequest request);
}
```

### 6.2 DefaultAiChatClient

```java
@RequiredArgsConstructor
public class DefaultAiChatClient implements AiChatClient {

    private final AiModuleStrategyRegistry strategyRegistry;
    private final AiProviderRouter aiProviderRouter;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        AiModuleStrategy strategy = strategyRegistry.get(request.getModule());
        AiChatRequest preparedRequest = strategy.prepare(request);
        AiProvider provider = aiProviderRouter.route(preparedRequest);
        return provider.chat(preparedRequest);
    }

    @Override
    public Flux<AiStreamChunk> streamChat(AiChatRequest request) {
        AiModuleStrategy strategy = strategyRegistry.get(request.getModule());
        AiChatRequest preparedRequest = strategy.prepare(request);
        AiProvider provider = aiProviderRouter.route(preparedRequest);
        return provider.streamChat(preparedRequest);
    }
}
```

### 6.3 AiModuleStrategy

`AiModuleStrategy` 是本次方案的核心，不再使用插槽接口。

```java
public interface AiModuleStrategy {

    /**
     * 当前策略支持的业务模块编码，例如 aichat。
     */
    String module();

    /**
     * 是否支持指定模块。
     */
    default boolean supports(String targetModule) {
        return module() != null && module().equalsIgnoreCase(targetModule);
    }

    /**
     * 对统一请求进行模块级标准化处理。
     * 例如补齐默认 provider、默认 model、默认 options，并做模块级参数校验。
     */
    AiChatRequest prepare(AiChatRequest request);
}
```

### 6.4 AiModuleStrategyRegistry

```java
public interface AiModuleStrategyRegistry {

    AiModuleStrategy get(String module);
}
```

推荐默认实现：

```java
@RequiredArgsConstructor
public class DefaultAiModuleStrategyRegistry implements AiModuleStrategyRegistry {

    private final List<AiModuleStrategy> strategies;

    @Override
    public AiModuleStrategy get(String module) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(module))
                .findFirst()
                .orElseThrow(() -> new AiModuleStrategyNotFoundException(module));
    }
}
```

### 6.5 AiProviderRouter

`AiProviderRouter` 不再按插槽路由，而是按“策略处理后的请求”选择 Provider。

```java
public interface AiProviderRouter {

    AiProvider route(AiChatRequest request);
}
```

推荐默认实现：

```java
@RequiredArgsConstructor
public class DefaultAiProviderRouter implements AiProviderRouter {

    private final List<AiProvider> providers;

    @Override
    public AiProvider route(AiChatRequest request) {
        String providerCode = request.getProvider();
        return providers.stream()
                .filter(provider -> provider.supports(providerCode))
                .findFirst()
                .orElseThrow(() -> new AiProviderNotFoundException(providerCode));
    }
}
```

### 6.6 AiProvider

```java
public interface AiProvider {

    String providerCode();

    default boolean supports(String targetProviderCode) {
        return providerCode() != null && providerCode().equalsIgnoreCase(targetProviderCode);
    }

    AiChatResponse chat(AiChatRequest request);

    Flux<AiStreamChunk> streamChat(AiChatRequest request);
}
```

---

## 7. ai-core 请求与响应模型

### 7.1 AiChatRequest

`AiChatRequest` 需要补充 `module` 字段，表示当前请求来自哪个业务模块。

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    /**
     * 请求唯一编号
     */
    private String requestId;

    /**
     * 业务模块编码，例如 aichat
     */
    private String module;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 模型供应商编码，例如 dashscope、kimi、fastapi-demo
     */
    private String provider;

    /**
     * 模型名称，例如 qwen-plus、moonshot-v1-8k
     */
    private String model;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户当前输入
     */
    private String userPrompt;

    /**
     * 历史上下文消息
     */
    private List<AiChatMessage> messages;

    /**
     * 温度
     */
    private BigDecimal temperature;

    /**
     * 最大输出 token
     */
    private Integer maxTokens;

    /**
     * 是否流式响应
     */
    private Boolean stream;

    /**
     * 扩展参数
     */
    private Map<String, Object> options;
}
```

### 7.2 AiChatMessage

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessage {

    private String role;

    private String content;
}
```

### 7.3 AiChatResponse

统一响应模型建议保持这些字段：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String requestId;

    private String provider;

    private String providerRequestId;

    private String model;

    private String content;

    private String reasoningContent;

    private String finishReason;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;

    private String rawResponse;
}
```

### 7.4 AiStreamChunk

流式响应模型建议保留统一语义：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamChunk {

    private String requestId;

    private String providerRequestId;

    private String delta;

    private String reasoningDelta;

    private Boolean finished;

    private String finishReason;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;

    private String rawChunk;
}
```

---

## 8. aichat 接入方式

### 8.1 aichat 的定位不变

`vita-aichat` 仍然只做业务编排，不进入 `ai-core` 内部，也不向 `ai-core` 下沉数据库能力。

当前保留内容：

```text
1. ai_conversation / ai_message / ai_chat_request / ai_message_stream_batch 落库
2. 并发治理、等待队列、会话锁
3. SSE 事件输出
4. 请求生命周期状态管理
5. 异常处理与状态回写
```

### 8.2 buildAiChatRequest 改造目标

`buildAiChatRequest()` 只负责把 `aichat` 业务上下文转换为统一请求，并明确：

```text
module = aichat
```

示例：

```java
private AiChatRequest buildAiChatRequest(PreparedSendContext context, AiMessageSendDto sendDto) {
    return AiChatRequest.builder()
            .requestId(context.getRequestId())
            .module("aichat")
            .conversationId(context.getConversation().getId())
            .userId(context.getUserId())
            .provider(resolveProvider(sendDto, context.getConversation()))
            .model(resolveModel(sendDto, context.getConversation()))
            .systemPrompt(resolveSystemPrompt(context.getConversation()))
            .userPrompt(sendDto.getContent())
            .messages(buildHistoryMessages(context.getConversation().getId()))
            .temperature(sendDto.getTemperature())
            .maxTokens(sendDto.getMaxTokens())
            .stream(Boolean.FALSE)
            .options(sendDto.getOptions())
            .build();
}
```

### 8.3 AichatModuleStrategy 的职责

当前阶段推荐在 `ai-core` 或与 `aichat` 对应的接入层中落地 `AichatModuleStrategy`，它的职责是：

```text
1. 校验 module=aichat 的请求是否完整
2. 补齐 aichat 默认 provider
3. 补齐 aichat 默认 model
4. 合并 aichat 默认 options
5. 对同步和流式请求做统一标准化
```

它不负责：

```text
1. 不查数据库
2. 不构建会话历史
3. 不做消息落库
4. 不做 Redis 锁控制
```

---

## 9. Provider 设计原则

### 9.1 DashScope Provider

`DashScope` 是当前正式业务基线，文档中必须以现有 `vita-aichat` 行为为准：

```text
1. 支持同步 chat
2. 支持流式 stream-chat
3. 保留 providerRequestId
4. 保留 finishReason
5. 保留 token usage
6. 保留 reasoning content / reasoning delta
7. 保持当前 Prompt 顺序语义不变
```

`DashScope Provider` 内部可以继续封装 Spring AI Alibaba，但这种封装应当放在 Provider 内，而不是放在 `vita-aichat` 中。

### 9.2 Kimi Provider

`Kimi` 作为正式业务 Provider，建议按 OpenAI-Compatible 方式适配：

```text
1. 提供同步 chat
2. 提供流式 stream-chat
3. 输出统一 AiChatResponse / AiStreamChunk
4. 不支持的 Provider 特性按“忽略而非抛错”处理
```

### 9.3 FastAPI Demo Provider

`FastAPI` 只作为 demo Provider，不应按正式生产能力描述。

它的职责边界仅限于：

```text
1. 演示 ai-core 如何接入一个自定义 HTTP AI 服务
2. 提供最小 chat 协议
3. 提供最小 stream-chat 协议
4. 演示统一响应映射和异常映射
```

它不承诺：

```text
1. 不承诺与 DashScope/Kimi 等价的能力完整性
2. 不承诺生产级 SLA
3. 不承诺复杂 reasoning、token usage、tool calling 等高级能力
```

---

## 10. 配置设计

### 10.1 总体原则

本次方案明确放弃数据库驱动模型配置，不再引入 `ai_model_config` 或类似配置表。

统一原则：

```text
1. ai-core 不耦合数据库
2. 模块策略不耦合数据库
3. Provider 不耦合数据库
4. 运行时配置统一通过 application.yml 管理
```

### 10.2 推荐配置结构

建议采用三层配置：

```yaml
vita:
  ai:
    core:
      modules:
        aichat:
          enabled: true
          provider: dashscope
          model: qwen-plus
          options:
            enable-search: true
            enable-thinking: false

spring:
  ai:
    providers:
      dashscope:
        enabled: true
        base-url: https://dashscope.aliyuncs.com
        api-key: your-dashscope-api-key
        default-model: qwen-plus

      kimi:
        enabled: true
        base-url: https://api.moonshot.cn/v1
        api-key: your-kimi-api-key
        default-model: moonshot-v1-8k

      fastapi-demo:
        enabled: false
        base-url: http://127.0.0.1:8000
        api-key: demo-key
        default-model: demo-chat-model

vita:
  aichat:
    default-system-prompt: 你是一个专业 AI 助手
    context-message-limit: 20
    stream-batch-max-chars: 64
    stream-batch-max-interval-ms: 200
```

### 10.3 配置边界

配置边界固定为：

```text
vita.ai.core.*
  管模块策略和默认路由

spring.ai.providers.*
  管具体 Provider 连接参数和默认模型

vita.aichat.*
  只管 aichat 业务配置
```

---

## 11. 属性类设计

### 11.1 AiCoreProperty

根配置类固定使用：

```text
AiCoreProperty
```

明确要求：

```text
1. AiCoreProperty 不允许定义静态内部类
2. 模块配置和 Provider 配置全部拆成顶级属性类
```

推荐拆分：

```text
AiCoreProperty
AiModuleProperty
AiModuleOptionProperty
DashScopeProviderProperty
KimiProviderProperty
FastApiDemoProviderProperty
```

### 11.2 自动装配建议

`vita-ai-core-spring-boot-starter` 负责自动装配：

```text
AiCoreProperty
AiModuleStrategyRegistry
AiProviderRouter
AiChatClient
```

各 Provider starter 负责各自 Provider Bean 和 Provider 属性类的装配。

---

## 12. Maven 依赖建议

`vita-aichat` 推荐依赖：

```xml
<dependency>
    <groupId>com.vita</groupId>
    <artifactId>vita-ai-core-spring-boot-starter</artifactId>
</dependency>

<dependency>
    <groupId>com.vita</groupId>
    <artifactId>vita-ai-provider-dashscope-spring-boot-starter</artifactId>
</dependency>

<dependency>
    <groupId>com.vita</groupId>
    <artifactId>vita-ai-provider-kimi-spring-boot-starter</artifactId>
</dependency>

<dependency>
    <groupId>com.vita</groupId>
    <artifactId>vita-ai-provider-fastapi-demo-spring-boot-starter</artifactId>
</dependency>
```

依赖方向固定为：

```text
vita-aichat -> ai-core starter
provider starter -> ai-core starter
ai-core starter -> vita-common
```

---

## 13. 升级实施步骤

### 第一阶段：重构 ai-core starter

完成：

```text
1. 删除插槽化表述和相关抽象
2. 固定 AiChatClient
3. 新增 AiModuleStrategy
4. 新增 AiModuleStrategyRegistry
5. 保留 AiProviderRouter 和 AiProvider
6. 统一异常体系
7. 完成 Spring Boot 3 自动装配
```

### 第二阶段：迁移 aichat

完成：

```text
1. aichat 只输出统一 AiChatRequest
2. request.module 固定为 aichat
3. send() 改为 AiChatClient.chat()
4. streamSend() 改为 AiChatClient.streamChat()
5. 业务层不再依赖 Provider SDK 类型
```

### 第三阶段：落地 DashScope Provider

完成：

```text
1. 迁移当前 DashScope 正式业务能力
2. 保留同步与流式语义
3. 保留 providerRequestId / finishReason / token usage / reasoning
```

### 第四阶段：落地 Kimi Provider

完成：

```text
1. 新增正式 Kimi Provider
2. 统一映射到 AiChatResponse / AiStreamChunk
3. 通过配置切换默认 Provider
```

### 第五阶段：补充 FastAPI Demo Provider

完成：

```text
1. 提供最小 demo chat 协议
2. 提供最小 demo stream-chat 协议
3. 演示自定义 HTTP AI 服务的接入方式
```

---

## 14. 最终推荐结论

最终建议采用以下结构：

```text
vita-aichat
  负责 AI Chat 业务编排

vita-ai-core-spring-boot-starter
  负责统一协议、模块策略、Provider 路由、异常体系、自动装配

vita-ai-provider-xxx-spring-boot-starter
  负责具体模型供应商实现
```

最终调用关系：

```text
AiChatController
 ↓
AiChatServiceImpl
 ↓
AiChatClient
 ↓
AiModuleStrategy
 ↓
AiProviderRouter
 ↓
AiProvider
 ↓
DashScope / Kimi / FastAPI Demo
```

一句话总结：

> aichat 做业务编排，ai-core starter 做模块策略和统一路由，provider starter 做模型适配，所有运行时配置由 application.yml 管理。

---

## 15. 升级收益

升级完成后可以获得以下收益：

```text
1. aichat 业务层不再被具体模型 SDK 污染
2. 不需要再拆 Scene Slot / Capability Slot，结构更稳定
3. 新增业务模块时只需要新增模块策略
4. 新增模型供应商时只需要新增 Provider starter
5. DashScope 和 Kimi 可以同时存在并通过配置切换
6. FastAPI demo 可以作为对外部 AI 服务的接入示例
7. 同步与流式能力保持统一协议
8. 统一异常体系更利于审计、限流、熔断和降级
9. 配置集中在 application.yml，更符合当前项目约束
10. ai-core 与 Provider 无数据库耦合，边界更清晰
```
