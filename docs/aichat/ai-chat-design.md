# be-vita AI Chat 设计文档

## 1. 目标

`vita-aichat` 作为独立启动模块，正式收敛到 `Spring AI Alibaba DashScope` 方案：

- 模型 starter：`spring-ai-alibaba-starter-dashscope`
- 配置前缀：`spring.ai.dashscope.*`
- 当前 provider：`spring.ai.model.chat=dashscope`
- 默认模型：`qwen-plus`
- 可选推理模型：`qwq-*`

本轮目标是让正式 `/chat/**` 接口直接依赖官方 `ChatModel` / `ChatClient` / `DashScopeChatOptions`，去掉旧的自定义网关层和 OpenAI-compatible 过渡实现。

## 2. 当前模块事实

### 2.1 模块与启动

- 模块：`vita-aichat`
- 启动类：`com.vita.config.VitaAIChatApplication`
- 上下文路径：`/aichat/api`
- 日志目录：`logs/vita-aichat/`

### 2.2 当前数据模型

正式 Chat 仍沿用现有三张表：

| 表名 | 实体 | 说明 |
| --- | --- | --- |
| `ai_conversation` | `AiConversation` | 会话主表，记录标题、模型、系统提示词、上下文参数 |
| `ai_message` | `AiMessage` | 消息表，记录用户消息、模型回复、状态、token、错误信息 |
| `ai_conversation_summary` | `AiConversationSummary` | 会话摘要表，记录历史压缩摘要 |

初始化脚本以 `sql/init/aichat.sql` 为准。

### 2.3 正式接口

当前正式接口只保留 `/chat/**`：

- `POST /chat/conversations`
- `GET /chat/conversations`
- `GET /chat/conversations/{conversationId}`
- `POST /chat/conversations/{conversationId}/title`
- `DELETE /chat/conversations/{conversationId}`
- `GET /chat/conversations/{conversationId}/messages`
- `POST /chat/conversations/{conversationId}/messages`
- `POST /chat/conversations/{conversationId}/messages/stream`

这些接口要求登录态，通过 `SaTokenConfigure` 全局鉴权后，再由 `LoginUserInfoModelContext` 取当前用户。

## 3. 技术方案

### 3.1 选型

正式模型接入方案：

- Spring Boot 3.x
- Spring AI 1.1.x
- Spring AI Alibaba DashScope
- MyBatis-Plus
- Sa-Token
- Redis
- SSE

不再保留旧的过渡网关、自定义 provider 前缀和 OpenAI-compatible 调用链。

### 3.2 官方配置入口

代码统一只读取 `spring.ai.*`：

```yaml
spring:
  ai:
    model:
      chat: dashscope
    default-system-prompt: ${AI_DEFAULT_SYSTEM_PROMPT:你是一个严谨的开发助手。}
    max:
      context:
        messages: ${AI_MAX_CONTEXT_MESSAGES:12}
    stream:
      enabled: ${AI_STREAM_ENABLED:true}
    connect:
      timeout: ${AI_CONNECT_TIMEOUT:5000}
    read:
      timeout: ${AI_READ_TIMEOUT:60000}
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY:}
      base-url: ${AI_DASHSCOPE_BASE_URL:https://dashscope.aliyuncs.com}
      chat:
        options:
          model: ${AI_DASHSCOPE_CHAT_MODEL:qwen-plus}
```

说明：

- `spring.ai.dashscope.base-url` 使用 DashScope 官方地址 `https://dashscope.aliyuncs.com`
- 官方 starter 会自动拼接 DashScope 原生 chat 路径
- `application-prod.yml` 里也直接定义同一套 `spring.ai.*` 键，并通过环境变量注入敏感值

### 3.3 最小自定义 Bean

本模块只保留少量业务必需 Bean：

- `AiChatProperties`
  - 绑定业务默认值：系统提示词、上下文条数、流式开关、超时
- `aiChatClient`
  - 基于官方 `ChatClient.builder(chatModel)` 构建
  - 当前主要用于后续 advisor / tool 扩展预留
- `aiChatExecutor`
  - 负责流式回复的异步执行
- `RestClientCustomizer` / `WebClientCustomizer`
  - 把 `spring.ai.connect.timeout`、`spring.ai.read.timeout` 应用到官方模型客户端
- 启动期 provider 校验
  - 校验 `spring.ai.model.chat`
  - 校验 `spring.ai.dashscope.api-key`
  - 校验 `spring.ai.dashscope.base-url`
  - 校验 `spring.ai.dashscope.chat.options.model`

## 4. 业务调用链路

### 4.1 创建会话

创建会话时：

- `userId` 从 `LoginUserInfoModelContext` 获取
- `title` 默认值为 `新对话`
- `titleSource=AUTO`
- `status=ACTIVE`
- `summaryStatus=NONE`
- `currentModelCode` 默认取 `spring.ai.dashscope.chat.options.model`

### 4.2 同步发送消息

同步发送链路：

1. 校验会话归属和状态
2. 写入一条 `USER` 消息
3. 写入一条 `ASSISTANT` 占位消息，状态为 `GENERATING`
4. 查询最新 `READY` 摘要
5. 查询最近 `N` 条可参与上下文的历史消息
6. 组装 `Prompt`
7. 构造 `DashScopeChatOptions`
8. 调用官方 `ChatModel.call(prompt)`
9. 读取回复文本、`reasoningContent`、usage
10. 回写助手消息状态为 `DONE`
11. 更新会话统计信息

### 4.3 流式发送消息

流式发送链路：

1. 与同步链路共用落库和 Prompt 组装逻辑
2. 使用 `ChatModel.stream(prompt)`
3. 按 chunk 提取正文增量和 `reasoningContent` 增量
4. 推送 SSE 事件：
   - `message-start`
   - `message-delta`
   - `reasoning-delta`
   - `message-end`
   - `error`
5. 流结束后落最终消息状态和 token 统计

## 5. Prompt 与模型规则

### 5.1 Prompt 组成

当前 Prompt 顺序固定为：

1. 会话系统提示词
2. 历史摘要（若存在）
3. 最近上下文消息
4. 当前用户消息

### 5.2 模型选择

- 会话创建时可显式传 `modelCode`
- 未传时默认使用 `qwen-plus`
- 后续消息直接沿用会话上的 `currentModelCode`

### 5.3 QwQ 推理模型

当 `modelCode` 命中 `qwq-*` 时：

- 自动在 `DashScopeChatOptions` 中设置 `enableThinking=true`
- 不新增单独的 `spring.ai.qwq.*` 配置
- 推理内容从官方返回的 `reasoningContent` 元数据读取
- 同步接口回写 `AiMessage.reasoningContent`
- 流式接口输出 `reasoning-delta`

## 6. 错误处理

统一错误码：

- `MODEL_AUTH_ERROR`
- `MODEL_TIMEOUT`
- `MODEL_RATE_LIMIT`
- `MODEL_PROVIDER_ERROR`
- `MODEL_NETWORK_ERROR`
- `MODEL_RESPONSE_INVALID`

映射策略：

- 401 / 403 -> `MODEL_AUTH_ERROR`
- 429 -> `MODEL_RATE_LIMIT`
- timeout -> `MODEL_TIMEOUT`
- 网络连接失败 -> `MODEL_NETWORK_ERROR`
- 空响应或结构异常 -> `MODEL_RESPONSE_INVALID`
- 其他供应商异常 -> `MODEL_PROVIDER_ERROR`

## 7. 代码结构

正式代码结构收口为：

```text
vita-aichat
├── aiConversation                  # 会话领域 entity/mapper/service
├── aiMessage                       # 消息领域 entity/mapper/service
├── aiConversationSummary           # 摘要领域 entity/mapper/service
├── chat
│   ├── dto                         # 正式接口请求 DTO
│   ├── model
│   │   ├── context                 # PreparedSendContext
│   │   └── result                  # AiChatCompletionResult
│   ├── service                     # IAiChatService / AiChatServiceImpl
│   ├── support                     # Assembler / SSE Writer
│   └── vo                          # 正式接口返回 VO
├── config                          # AiChatProperties / AiChatConfiguration
└── controller/aichat              # AiChatController
```

不再保留独立 `gateway` 层。

## 8. 后续扩展边界

未来扩展新模型时，优先遵循以下顺序：

1. 增加对应官方 starter
2. 增加对应官方配置前缀
3. 通过 `spring.ai.model.chat` 切换 provider
4. 业务层继续只依赖官方 `ChatModel` / `ChatClient`

当前不做：

- 自动摘要刷新任务
- 自动会话命名
- 多 provider 运行时动态热切换
- 自定义网关协议层

## 9. 验收标准

- 正式接口只保留 `/chat/**`
- 代码只认 `spring.ai.dashscope.*`
- `qwq-*` 支持 `enableThinking` 和 `reasoningContent`
- 同步与流式链路都基于官方 `ChatModel`
- `application-prod.yml` 不再保留明文 DashScope Key
