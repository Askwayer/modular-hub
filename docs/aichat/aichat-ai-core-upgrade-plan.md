# AIChat 模块升级方案：多模型接入、异步流式与记忆压缩

## 1. 升级背景

当前 `vita-aichat` 已完成正式 Chat 业务链路，已经具备：

```text
1. 会话、消息、请求生命周期、流式批次落库
2. Spring MVC + SseEmitter 流式接口
3. Redis 并发治理、会话锁、等待队列
4. 统一 AiChatClient / AiProvider / AiModuleStrategy 抽象
5. DashScope 正式业务调用链路
```

但现有 AI 接入层还存在几个明显问题：

```text
1. 业务侧依赖分成了 vita-ai-core-spring-boot-starter 和 vita-ai-provider-spring-boot-starter 两层，使用入口偏碎
2. provider starter 已同时引入 DashScope 和 OpenAI 依赖，但自动装配仍按通用 ChatModel 注入，无法安全支撑多 Provider 同时加载
3. 当前配置和文档仍默认“单一 dashscope 模型”，不能清晰表达同一业务在 Qwen/Kimi 间切换
4. ai_conversation_summary 目前只有读取能力，尚未形成“每轮摘要 + 多轮压缩”的正式记忆闭环
```

因此，本次升级目标不是继续拆更多 starter，而是反向收敛：

> 对业务侧合并为单一 `vita-ai-spring-boot-starter`，内部统一承载多模型接入、Provider 路由、异步流式、轮次摘要和上下文压缩能力。

---

## 2. 升级目标

本次升级后的总体目标如下：

```text
1. 单个业务同时加载 Qwen(DashScope) 和 Kimi(OpenAI-Compatible)
2. 后续可继续扩展其他大模型，不需要改 aichat 主业务链路
3. 对外只保留一个 AI starter，不再把业务接入拆成 core/provider 两个 Maven 入口
4. 流式接口继续保留 Spring MVC + SseEmitter，并明确异步返回能力
5. 每轮对话结束后异步生成一条轮次摘要消息
6. 多条摘要消息达到阈值后，再异步压缩成会话级摘要
7. 压缩阈值、摘要模型、上下文压缩策略可配置
8. 摘要与压缩失败不影响主对话成功返回
```

---

## 3. 推荐最终模块结构

推荐将当前两层 starter 收敛为单一业务入口：

```text
be-vita
├── vita-aichat
│   └── AI Chat 业务编排、会话、消息、SSE、落库、摘要任务触发
│
├── vita-starters
│   └── vita-ai-spring-boot-starter
│       ├── client
│       │   └── AiChatClient
│       ├── router
│       │   └── AiProviderRouter
│       ├── strategy
│       │   ├── AiModuleStrategy
│       │   └── AichatModuleStrategy
│       ├── provider
│       │   ├── dashscope
│       │   ├── kimi
│       │   └── fastapi
│       ├── model
│       │   ├── AiChatRequest
│       │   ├── AiChatResponse
│       │   ├── AiStreamChunk
│       │   └── AiChatMessage
│       ├── property
│       ├── config
│       └── exception
│
└── vita-common
    └── 公共异常、上下文、基础工具
```

这里的设计原则固定为：

```text
1. 对业务使用方只暴露一个 starter：vita-ai-spring-boot-starter
2. 内部保留 provider 包分层，避免代码混成一层
3. 不再额外拆多个 AI starter Maven 模块
4. DashScope、Kimi、FastAPI 通过内部 Provider 适配器并存
```

---

## 4. 现状问题与改造结论

### 4.1 现状问题

当前代码层面的真实问题如下：

```text
1. vita-ai-provider-spring-boot-starter 已同时依赖：
   - spring-ai-alibaba-starter-dashscope
   - spring-ai-starter-model-openai

2. DashScopeProviderAutoConfiguration 仍然按 ChatModel 注入：
   public DashScopeProvider dashScopeProvider(ChatModel chatModel)

3. 一旦 DashScope 和 Kimi 同时启用，ChatModel Bean 可能出现多实现歧义

4. aichat 当前 DTO、会话表、请求表主要只有 model_code，没有稳定的 provider_code 路由来源

5. ai_conversation_summary 目前只有读取和删除，没有正式的生成、失效、再次压缩闭环
```

### 4.2 升级结论

因此本次代码升级文档固定以下结论：

```text
1. 合并为单一 AI starter 是合理的，不属于过度合并
2. 多模型同时加载必须改成 Provider 专属装配，不能继续注入泛型 ChatModel
3. 路由必须显式区分 providerCode 和 modelCode
4. 记忆能力必须拆成“轮次摘要”和“摘要压缩”两层
5. 摘要与压缩都采用异步后台执行
```

---

## 5. 核心分层职责

### 5.1 vita-aichat：业务编排层

`vita-aichat` 继续只负责业务侧职责：

```text
1. 用户与会话校验
2. requestId 幂等控制
3. 会话锁、等待队列、并发治理
4. 用户消息和助手消息落库
5. 流式批次落库
6. SSE 事件输出
7. 主对话完成后的摘要任务触发
8. 失败状态回写
```

不再直接承担：

```text
1. 不直接使用 DashScopeChatOptions
2. 不直接调用 OpenAI/Kimi SDK
3. 不直接解析各 Provider 原始响应
4. 不在业务层手写不同 Provider 的 if/else
```

### 5.2 vita-ai-spring-boot-starter：统一 AI 接入层

统一 starter 负责：

```text
1. 统一请求/响应协议
2. 模块策略处理
3. Provider 路由
4. DashScope Provider 适配
5. Kimi Provider 适配
6. FastAPI demo Provider 适配
7. 自动装配与配置绑定
8. 统一异常体系
```

### 5.3 模块策略

保留 `AiModuleStrategy`，但它只负责业务模块标准化，不负责数据库：

```text
1. 根据 module=aichat 补齐默认 provider/model
2. 校验当前业务允许使用的 Provider
3. 统一整理 options
4. 为同步和流式请求输出标准化 AiChatRequest
```

---

## 6. 多模型接入设计

### 6.1 支持同时加载的模型

当前阶段正式支持：

```text
1. Qwen：通过 DashScope 接入
2. Kimi：通过 OpenAI-Compatible 接入
3. FastAPI：仅 demo，不作为正式生产 Provider
```

对应依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

### 6.2 路由原则

Provider 路由来源统一为：

```text
1. sendDto.providerCode / sendDto.modelCode
2. conversation.currentProviderCode / currentModelCode
3. vita.ai.modules.aichat.default-provider / default-model
```

即：

```text
请求覆盖 > 会话默认 > 模块默认
```

### 6.3 Provider 装配原则

必须废弃“注入泛型 ChatModel”这种方式，升级为：

```text
1. DashScope Provider 使用 DashScope 专属适配器或具名 Bean
2. Kimi Provider 使用 OpenAI 专属适配器或具名 Bean
3. AiProviderRouter 只根据 providerCode 选择 Provider
4. Provider 内部再处理各自 SDK 的请求转换与响应映射
```

这样才能支持 DashScope 和 Kimi 同时在 classpath 中存在且同时启用。

### 6.4 后续扩展方式

未来新增其他模型时，遵循同一模式：

```text
1. 新增一个 Provider 适配实现
2. 增加一组 Provider 配置
3. 注册到 AiProviderRouter
4. 不改 vita-aichat 主业务流程
```

---

## 7. 统一请求与公开类型调整

### 7.1 AiConversationCreateDto

建议新增：

```text
providerCode
modelCode
```

用途：

```text
1. 创建会话时指定默认 Provider
2. 创建会话时指定默认 Model
```

### 7.2 AiMessageSendDto

建议新增：

```text
providerCode
modelCode
```

用途：

```text
1. 当前轮临时切换 Provider
2. 当前轮临时切换 Model
3. 不改会话默认设置即可切换 Qwen/Kimi
```

### 7.3 AiChatRequest

统一请求模型保留以下关键字段：

```text
module
provider
model
systemPrompt
messages
userPrompt
temperature
maxTokens
stream
options
```

这里明确：

```text
provider = providerCode
model = modelCode
module 固定为 aichat
```

---

## 8. 数据表升级设计

### 8.1 ai_conversation

现有只有 `current_model_code`，升级后增加：

```text
current_provider_code
```

职责：

```text
1. 标识当前会话默认走哪个 Provider
2. 与 current_model_code 一起构成稳定的会话级模型路由
```

### 8.2 ai_chat_request

现有增加：

```text
provider_code
request_scene
```

其中 `request_scene` 用于区分：

```text
CHAT
ROUND_SUMMARY
SUMMARY_COMPRESS
```

### 8.3 ai_message

现有增加：

```text
provider_code
```

并明确：

```text
role_type=SUMMARY
用于存储每轮对话结束后生成的轮次摘要
```

### 8.4 ai_conversation_summary

现有增加：

```text
provider_code
model_code
```

用途：

```text
1. 记录本次压缩摘要由哪个 Provider/Model 生成
2. 便于后续审计和问题排查
```

---

## 9. 流式接口与异步返回设计

### 9.1 当前事实

当前 `sendMessageStream()` 已经具备异步返回能力：

```text
1. Controller 返回 SseEmitter
2. HTTP 线程先结束
3. 模型流式消费在 aiChatTaskExecutor 中执行
4. Provider 通过 Flux<AiStreamChunk> 返回增量结果
5. aichat 持续推送 SSE 事件并批次落库
```

### 9.2 升级结论

本次升级文档明确保留：

```text
Spring MVC + SseEmitter + 独立线程池 + Provider 流式消费
```

不切换到：

```text
1. 不改成 WebFlux Controller
2. 不要求接口签名改成 Flux<ServerSentEvent<?>>
```

### 9.3 与摘要的关系

必须增加一个明确约束：

```text
1. 主流式结束并发送 message-end 后，才能触发轮次摘要
2. 摘要生成和摘要压缩不能阻塞 SSE 完成
3. 摘要和压缩必须在事务提交后异步执行
```

---

## 10. 记忆摘要与上下文压缩设计

### 10.1 总体思路

会话记忆分两层：

```text
第一层：轮次摘要
第二层：压缩摘要
```

### 10.2 第一层：轮次摘要

每轮问答成功后，异步生成一条摘要消息：

```text
1. 输入：本轮用户消息 + 本轮助手消息 + 最近摘要上下文
2. 输出：一条 role_type=SUMMARY 的 ai_message
3. 用途：作为中间记忆单元，供后续再压缩
```

触发时机：

```text
1. 同步回复成功后触发
2. 流式回复成功并完成最终回写后触发
3. 使用 AFTER_COMMIT 异步事件执行
```

### 10.3 第二层：摘要压缩

当未压缩的 `SUMMARY` 消息累计达到阈值后，再异步执行压缩：

```text
1. 读取最近一批未压缩 SUMMARY 消息
2. 调用摘要压缩模型
3. 生成新的 ai_conversation_summary
4. 将之前的 SUMMARY 消息标记为已压缩或移出下一轮压缩范围
```

### 10.4 Prompt 组装顺序

升级后 Prompt 顺序建议调整为：

```text
1. systemPrompt
2. 最新 READY 的压缩摘要
3. 尚未被压缩的 SUMMARY 消息
4. 最近原始上下文消息
5. 当前用户消息
```

这比“只带最新一条 conversation summary + 原始消息”更适合长期会话记忆。

### 10.5 压缩触发条件

压缩条件设计为可配置：

```text
1. 未压缩 SUMMARY 消息条数阈值
2. 预计上下文 token 阈值
3. 可选强制压缩开关
```

### 10.6 摘要模型配置

摘要和压缩支持独立模型配置：

```text
1. 可与主对话模型相同
2. 也可单独指定 providerCode/modelCode
3. 例如主对话用 qwen-plus，摘要压缩用 qwen-turbo 或 kimi 短上下文模型
```

### 10.7 失败处理原则

摘要和压缩都遵循：

```text
1. 失败不回滚主对话结果
2. 失败只更新 summaryStatus 或请求状态
3. 可重试，但不影响用户本轮可见回复
```

---

## 11. 配置体系设计

### 11.1 配置边界

升级后配置重新分层：

```yaml
vita:
  ai:
    modules:
      aichat:
        enabled: true
        default-provider: qwen
        default-model: qwen-plus
        allowed-providers:
          - qwen
          - kimi

    providers:
      fastapi:
        enabled: false
        base-url: http://127.0.0.1:8000
        api-key: demo-key

  aichat:
    stream:
      enabled: true
    concurrency:
      max-active: 5
      max-waiting: 20
    summary:
      enabled: true
      async: true
      round-summary-model-provider: qwen
      round-summary-model-code: qwen-plus
      compress-enabled: true
      compress-threshold-count: 10
      compress-threshold-tokens: 12000
      compress-model-provider: qwen
      compress-model-code: qwen-plus

spring:
  ai:
    providers:
      qwen:
        enabled: true
        api-key: your-qwen-key
        base-url: https://dashscope.aliyuncs.com
        default-model: qwen-plus
      kimi:
        enabled: true
        api-key: your-kimi-key
        base-url: https://api.moonshot.cn/v1
        default-model: kimi-k2.5
```

### 11.2 配置规则

文档中明确：

```text
1. spring.ai.providers.qwen.* / spring.ai.providers.kimi.* 只管 Provider 连接配置
2. 不再使用 spring.ai.dashscope.* / spring.ai.openai.* 作为本 starter 的有效输入
3. vita.ai.modules.aichat.* 只管业务模块默认路由
4. vita.aichat.summary.* 只管记忆摘要与压缩策略
5. FastAPI demo 使用 vita.ai.providers.fastapi.* 管理
```

### 11.3 属性类约束

当前 `AiChatProperty` 使用深层静态内部类，不适合继续承载摘要与压缩配置。

升级文档中要求：

```text
1. AiChatProperty 拆为多个顶级属性类
2. 不继续使用深层静态内部类承载复杂配置
3. starter 和 aichat 分别维护自己的配置类边界
```

---

## 12. 关键接口设计

### 12.1 AiChatClient

保持统一入口：

```java
public interface AiChatClient {

    AiChatResponse chat(AiChatRequest request);

    Flux<AiStreamChunk> streamChat(AiChatRequest request);
}
```

### 12.2 AiProvider

保留统一接口，但内部实现必须按 Provider 专属装配：

```java
public interface AiProvider {

    String providerCode();

    AiChatResponse chat(AiChatRequest request);

    Flux<AiStreamChunk> streamChat(AiChatRequest request);
}
```

### 12.3 记忆任务接口

建议新增两类内部服务接口：

```text
1. RoundSummaryService
   负责生成每轮摘要消息

2. ConversationSummaryCompressService
   负责多条摘要的再次压缩
```

它们都由 `vita-aichat` 异步触发，但模型调用仍走统一 `AiChatClient`。

---

## 13. 升级实施步骤

### 第一阶段：收敛 starter

```text
1. 将现有 ai-core/provider 文档与代码入口收敛为 vita-ai-spring-boot-starter
2. 内部保留 provider 包结构
3. 调整自动装配，去掉泛型 ChatModel 注入
```

### 第二阶段：完善多模型路由

```text
1. DTO 增加 providerCode/modelCode
2. 会话、消息、请求表增加 provider_code
3. 打通请求覆盖 > 会话默认 > 模块默认 路由优先级
```

### 第三阶段：保留并强化异步流式

```text
1. 保留现有 SseEmitter + 独立线程池模式
2. 明确 Provider 流式能力统一走 Flux<AiStreamChunk>
3. 保证主流式结束与摘要任务解耦
```

### 第四阶段：落地轮次摘要

```text
1. 每轮成功回复后异步生成一条 SUMMARY 消息
2. 统一纳入 ai_chat_request 生命周期
3. 摘要失败不影响主业务结果
```

### 第五阶段：落地摘要压缩

```text
1. 达到阈值后触发 ai_conversation_summary 压缩
2. Prompt 组装引入“压缩摘要 + 未压缩 SUMMARY + 最近原始消息”
3. 支持上下文压缩阈值配置
```

---

## 14. 最终推荐结论

最终建议固定为：

```text
1. 对业务侧合并为一个 starter，而不是继续拆 core/provider 两层入口
2. DashScope 和 Kimi 同时加载是必须支持的标准能力
3. 流式接口继续保持异步返回，不切 WebFlux Controller
4. 记忆能力按“每轮摘要 + 多轮压缩”两层设计
5. 摘要与压缩都异步执行，不阻塞主对话
```

一句话总结：

> aichat 负责业务编排、SSE 与落库；统一 AI starter 负责多模型接入与路由；对话记忆通过异步轮次摘要和多轮压缩实现，支持长期上下文控制。
