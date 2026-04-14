# be-vita AI Chat 设计文档

## 1. 文档目标

本文档以当前仓库真实结构为基础，定义 `vita-aichat` 模块的正式设计与落地边界。目标不是重新设计一个并行 AI 模块，而是在现有 `vita-aichat` 和现有三张表上，明确正式 Chat 能力、Qwen 模型接入方案和后续演进顺序。

本轮定稿范围：

- 基于现有 `vita-aichat` 模块建设正式 Chat 业务接口
- 保留 `AiModelGatewayService` 作为统一模型网关抽象
- 首个对接模型定为 `Qwen`
- 接入主线定为 `Spring AI`
- `Qwen` 通过 `Spring AI OpenAI starter + OpenAI-compatible` 接口接入
- 当前仅保留正式 Chat 接口，不再维护生成式 CRUD 入口

---

## 2. 当前仓库现状

### 2.1 模块与启动方式

- 模块名：`vita-aichat`
- 启动类：`com.vita.config.VitaAIChatApplication`
- 上下文路径：`/aichat/api`
- 日志目录：`logs/vita-aichat/`
- Maven 依赖：当前直接依赖 `vita-common`

### 2.2 已存在的领域对象

当前 `vita-aichat` 已存在三张核心表，对应三组实体：

| 表名 | 实体 | 用途 |
| --- | --- | --- |
| `ai_conversation` | `AiConversation` | 会话主表，记录标题、当前模型、上下文参数、摘要版本等 |
| `ai_message` | `AiMessage` | 消息表，记录用户消息、模型回复、状态、token、错误信息等 |
| `ai_conversation_summary` | `AiConversationSummary` | 摘要表，记录历史消息压缩结果和摘要版本 |

初始化脚本以 [sql/init/aichat.sql](/sql/init/aichat.sql) 为准。

### 2.3 当前正式能力

当前 `vita-aichat` 已经具备以下正式能力：

- `/chat/**` 任务导向接口
- 基于 `Spring AI` 的 `Qwen OpenAI-compatible` 模型调用
- 同步发送消息与 SSE 流式回复
- 消息占位落库、状态流转和统一错误码映射
- 摘要参与上下文组装

### 2.4 当前仍保留的后续能力

当前暂未落地、但文档保留为后续演进项的能力有：

- 自动标题生成闭环
- 主动摘要刷新任务
- 更细粒度的流式增量持久化

---

## 3. 设计原则

- 以现有 `vita-aichat` 模块为基础演进，不新增平行模块。
- 会话、消息、摘要三表继续沿用，不重建第二套数据模型。
- 业务层只依赖统一模型网关抽象，不直接耦合厂商 SDK。
- 模型接入框架统一收敛到 `Spring AI`，不并行维护第二套调用框架。
- 正式接口走任务导向，不再保留并行 CRUD 控制器。
- 所有会话和消息都以当前登录用户为归属边界，核心归属字段使用 `user_id`。
- 流式场景采用“先写占位消息，再增量回写”的持久化策略。

---

## 4. 模块边界与包结构建议

### 4.1 当前结构

现有包结构继续保留：

```text
vita-aichat
├── src/main/java/com/vita/config
├── src/main/java/com/vita/controller/aichat
└── src/main/java/com/vita/aichat
    ├── aiConversation
    ├── aiMessage
    └── aiConversationSummary
```

### 4.2 当前建议结构

围绕正式 Chat 链路的推荐结构如下：

```text
com.vita.aichat
├── aiConversation                 # 保留现有会话领域对象
├── aiMessage                      # 保留现有消息领域对象
├── aiConversationSummary          # 保留现有摘要领域对象
├── chat                           # 正式 Chat 任务接口 DTO / VO / Service
├── gateway                        # AiModelGatewayService 及 provider 适配
└── support                        # 组装器、SSE 写出器等纯辅助能力
```

边界约束：

- `com.vita.controller.aichat` 承载接口层，不做模型调用与上下文拼装。
- `com.vita.aichat.gateway` 只负责供应商协议适配，不承载业务判断。
- `com.vita.aichat.chat` 负责正式 Chat 任务流程编排。
- 可复用的基础能力继续留在 `vita-common`。

---

## 5. 数据模型说明

### 5.1 `ai_conversation`

关键字段：

- `user_id`：会话归属用户
- `title`、`title_source`：标题及来源，避免人工标题被自动覆盖
- `status`：`ACTIVE` / `ARCHIVED` / `DELETED`
- `current_model_id`、`current_model_code`：当前模型信息
- `system_prompt`、`temperature`、`max_tokens`：本会话模型参数
- `max_context_messages`：上下文窗口条数上限
- `summary_version`、`summary_status`：摘要版本与状态

### 5.2 `ai_message`

关键字段：

- `conversation_id`、`user_id`：消息归属
- `role_type`：`USER` / `ASSISTANT` / `SYSTEM` / `SUMMARY`
- `message_type`：当前先以 `TEXT` 为主，预留图片和文件
- `request_id`：一次调用链路标识
- `model_id`、`model_code`：本次回复使用的模型
- `content`、`reasoning_content`：展示内容与推理内容
- `content_status`：`GENERATING` / `DONE` / `ERROR` / `CANCELLED`
- `token_input`、`token_output`、`token_total`：token 统计
- `error_code`、`error_message`：模型失败信息

### 5.3 `ai_conversation_summary`

关键字段：

- `conversation_id`、`user_id`：摘要归属
- `version_no`：摘要版本号
- `source_message_start_id`、`source_message_end_id`：被压缩的消息范围
- `summary_text`、`summary_json`：摘要内容
- `token_saved_estimate`：预估节省 token
- `status`：`READY` / `STALE`

结论：

- 三表模型已足够支持文本对话、上下文压缩、自动标题和后续多模态扩展。
- 本轮不新增“模型配置表”或“会话上下文快照表”，模型选择先通过 `current_model_code` / `model_code` 管理。

---

## 6. 正式接口契约

正式 Chat 接口统一挂在 `/aichat/api/chat` 下，代码中只保留这些正式接口。

### 6.1 会话接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/chat/conversations` | 创建会话 |
| `GET` | `/chat/conversations` | 查询会话列表 |
| `GET` | `/chat/conversations/{conversationId}` | 查询会话详情 |
| `PATCH` | `/chat/conversations/{conversationId}/title` | 重命名会话 |
| `DELETE` | `/chat/conversations/{conversationId}` | 删除会话 |

### 6.2 消息接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/chat/conversations/{conversationId}/messages` | 查询历史消息 |
| `POST` | `/chat/conversations/{conversationId}/messages` | 发送消息并等待完整回复 |
| `POST` | `/chat/conversations/{conversationId}/messages/stream` | 发送消息并通过 SSE 返回流式回复 |

### 6.3 创建会话请求

```json
{
  "title": "新对话",
  "systemPrompt": "你是一个严谨的开发助手。",
  "modelCode": "qwen-plus",
  "temperature": 0.7,
  "maxTokens": 4096,
  "maxContextMessages": 12
}
```

说明：

- `title` 可为空，默认写入“新对话”。
- `modelCode` 本轮默认使用 `qwen-*` 系列。
- 当前不要求前端传 `modelId`，避免在第一版引入额外模型管理复杂度。

### 6.4 发送消息请求

```json
{
  "content": "帮我总结这段代码的职责边界。",
  "messageType": "TEXT",
  "useContext": 1,
  "requestId": "chat-20260413-0001"
}
```

同步响应建议：

```json
{
  "conversationId": 10001,
  "userMessageId": 20001,
  "assistantMessageId": 20002,
  "requestId": "chat-20260413-0001",
  "contentStatus": "DONE",
  "modelCode": "qwen-plus",
  "content": "这里是完整回复内容",
  "reasoningContent": null,
  "tokenInput": 256,
  "tokenOutput": 512,
  "tokenTotal": 768
}
```

### 6.5 SSE 事件约定

流式接口返回 `text/event-stream`，建议事件类型如下：

| 事件名 | 说明 |
| --- | --- |
| `message-start` | 模型回复占位消息已创建 |
| `message-delta` | 追加正文片段 |
| `reasoning-delta` | 追加推理内容片段，可选 |
| `message-end` | 回复完成，返回最终状态与 token 统计 |
| `error` | 模型调用失败或流中断 |

约束：

- 发起流式调用前必须先落库一条 `ASSISTANT` 占位消息，状态为 `GENERATING`。
- 发生异常时回写 `content_status = ERROR`，并记录 `error_code`、`error_message`。

---

## 7. 核心业务流程

### 7.1 创建会话

1. 获取当前登录用户
2. 校验模型编码和参数范围
3. 写入 `ai_conversation`
4. 返回会话 ID 和默认配置

### 7.2 发送消息并流式回复

1. 校验 `conversationId` 归属当前用户
2. 写入 `USER` 消息
3. 创建 `ASSISTANT` 占位消息，状态设为 `GENERATING`
4. 组装系统提示词、摘要、最近消息作为上下文
5. 调用 `AiModelGatewayService` 发起流式请求
6. 通过 SSE 输出 `message-start`、`message-delta`、`reasoning-delta`
7. 完成后统一回写 token 统计、消息状态、会话统计信息

### 7.3 摘要刷新（后续能力）

触发时机：

- 历史消息数超过 `max_context_messages`
- 或 token 估算超过模型上下文窗口阈值

处理步骤：

1. 读取待压缩历史消息
2. 调用网关生成摘要
3. 新增一条 `ai_conversation_summary`
4. 更新 `ai_conversation.summary_version`
5. 将 `summary_status` 更新为 `READY`

### 7.4 自动标题（后续能力）

约束：

- 仅在 `title_source = AUTO` 时允许覆盖标题
- 仅在首轮或前几轮消息后触发，避免频繁改名
- 标题生成失败不影响主对话链路

---

## 8. 模型网关与 Qwen 接入

### 8.1 网关抽象

本模块正式模型接入基于 `Spring AI`。业务层通过 `AiModelGatewayService` 屏蔽具体供应商，网关实现内部使用 `Spring AI ChatModel` 或 `ChatClient` 调用模型。

保留统一模型网关接口：

```java
public interface AiModelGatewayService {

    void chatStream(AiChatGatewayReq req, AiStreamListener listener);

    AiChatGatewayResp chat(AiChatGatewayReq req);
}
```

业务层只依赖这层抽象，不直接感知具体供应商。本期只实现对话同步调用和流式调用。

### 8.2 首个 provider：Qwen

本轮首个接入模型固定为 `Qwen`，实现方式为：

- 框架：`Spring AI`
- 模型 starter：`spring-ai-starter-model-openai`
- 供应商协议：`OpenAI-compatible`
- 目标地址：DashScope 兼容接口

约束：

- 业务层统一传 `modelCode`
- 第一版默认模型可使用 `qwen-plus`
- 其他 `qwen-*` 型号通过配置切换，不改业务代码

### 8.3 配置契约

建议配置如下：

```yaml
spring:
  ai:
    model:
      chat: ${vita.aichat.model.chat:openai}
    openai:
      api-key: ${vita.aichat.openai.api-key:}
      base-url: ${vita.aichat.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}
      chat:
        options:
          model: ${vita.aichat.openai.chat.options.model:qwen-plus}

vita:
  aichat:
    default-system-prompt: ${VITA_AICHAT_DEFAULT_SYSTEM_PROMPT:你是一个严谨的开发助手。}
    default-model-code: ${vita.aichat.openai.chat.options.model:qwen-plus}
    max-context-messages: ${VITA_AICHAT_MAX_CONTEXT_MESSAGES:12}
    stream-enabled: ${VITA_AICHAT_STREAM_ENABLED:true}
    connect-timeout-ms: ${VITA_AICHAT_CONNECT_TIMEOUT_MS:5000}
    read-timeout-ms: ${VITA_AICHAT_READ_TIMEOUT_MS:60000}
```

补充说明：

- 面向中国区默认使用 `https://dashscope.aliyuncs.com/compatible-mode/v1`
- 如部署在海外，可按环境切换到对应国际区兼容地址
- `api-key` 不允许写死在仓库中，必须走环境变量或外部配置
- 模型配置收敛为 `spring.ai.openai.*` + `vita.aichat.*` 两层，不再维护 `langchain4j.*` 的并行配置

### 8.4 请求映射

网关层基于 Spring AI 组装 `Prompt` 与 `OpenAiChatOptions`，再映射成 OpenAI-compatible 请求：

- `modelCode -> model`
- `systemPrompt + summary + recent messages -> messages`
- `temperature -> temperature`
- `maxTokens -> max_tokens`
- 流式请求时设置 `stream = true`

### 8.5 错误码映射

建议统一错误码：

| 外部错误 | 内部错误码 |
| --- | --- |
| 401 / 403 | `MODEL_AUTH_ERROR` |
| 408 / 504 | `MODEL_TIMEOUT` |
| 429 | `MODEL_RATE_LIMIT` |
| 5xx | `MODEL_PROVIDER_ERROR` |
| 网络异常 / 连接失败 | `MODEL_NETWORK_ERROR` |
| 响应解析失败 | `MODEL_RESPONSE_INVALID` |

落库规则：

- `error_code` 存统一错误码
- `error_message` 存供应商摘要错误信息
- 不直接把完整密钥、原始请求头、原始异常栈写进消息内容

---

## 9. 安全、归属与状态流转

### 9.1 用户归属

- 会话和消息查询必须按当前登录用户过滤 `user_id`
- 不允许跨用户读取或写入会话
- `conversationId` 只能在用户自己的数据范围内生效

### 9.2 消息状态流转

状态机：

```text
GENERATING -> DONE
GENERATING -> ERROR
GENERATING -> CANCELLED
```

约束：

- 只有 `ASSISTANT` 消息允许进入 `GENERATING`
- `USER` 消息默认直接写为完成态
- 取消和超时都要保留消息轨迹，不能直接物理删除

### 9.3 审计与链路

- 每次发送消息建议带 `requestId`
- `requestId` 同步写入消息表，便于日志和异常定位
- 重要事件要记录：会话创建、消息发送、模型失败、摘要刷新、标题更新

---

## 10. 开发顺序

建议按以下顺序推进：

1. 保持现有三表和实体不变，补正式 Chat DTO / VO / Service
2. 保持 `/chat/**` 任务导向控制器为唯一正式入口
3. 落 `AiModelGatewayService` 的 `Spring AI + Qwen OpenAI-compatible` 实现
4. 完成发送消息、流式回写、消息状态流转
5. 后续补摘要刷新和自动标题
6. 最后补限流、审计日志和扩展测试

---

## 11. 验收标准

- 文档中的模块名、表名、包名、启动类、上下文路径与当前源码一致
- 不再保留旧模块名、旧表名前缀和旧模型接入表述
- 正式接口契约明确为任务导向，代码中不再保留并行 CRUD 控制器
- `Qwen` 被写成首个落地模型，接入基于 `Spring AI + OpenAI-compatible`
- 配置契约、错误码映射、SSE 事件、消息状态流转都有明确约定

---

## 12. 结论

`vita-aichat` 当前已经具备三表模型、正式 Chat 接口和 `Spring AI + Qwen OpenAI-compatible` 模型接入。后续重点不再是重建结构，而是围绕摘要刷新、自动标题和更细粒度的流式持久化继续完善。
