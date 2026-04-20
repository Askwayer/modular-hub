package com.vita.aichat.chat.model.context;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiMessage.entity.AiMessage;
import lombok.Data;

/**
 * 统一发送消息前的准备上下文。
 */
@Data
public class PreparedSendContext {

    /* 用户ID */
    private Long userId;

    /* 会话ID */
    private Long conversationId;

    /* 请求唯一标识 */
    private String requestId;

    /* 模型编码 */
    private String modelCode;

    /* 会话实体 */
    private AiConversation conversation;

    /* 用户消息实体 */
    private AiMessage userMessage;

    /* 助手消息实体 */
    private AiMessage assistantMessage;

    /* 聊天请求实体（数据库） */
    private AiChatRequest chatRequest;

    /* 统一 AI 请求 DTO（协议） */
    private com.vita.ai.core.model.AiChatRequest aiChatRequest;

    /* 活跃并发名额ID */
    private String activePermitId;

    /* 是否经历过等待 */
    private boolean waited;
}
