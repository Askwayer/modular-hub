package com.vita.aichat.chat.model.context;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiMessage.entity.AiMessage;
import lombok.Getter;
import lombok.Setter;

/**
 * 持有一次消息发送链路中的会话、用户消息和助手占位消息。
 */
@Getter
@Setter
public class PreparedSendContext {
    private Long userId;
    private String requestId;
    private AiConversation conversation;
    private AiMessage userMessage;
    private AiMessage assistantMessage;
}
