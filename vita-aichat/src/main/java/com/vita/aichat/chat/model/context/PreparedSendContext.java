package com.vita.aichat.chat.model.context;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiMessage.entity.AiMessage;
import lombok.Data;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * 统一发送消息前的准备上下文。
 */
@Data
public class PreparedSendContext {

    private Long userId;

    private Long conversationId;

    private String requestId;

    private String modelCode;

    private AiConversation conversation;

    private AiMessage userMessage;

    private AiMessage assistantMessage;

    private AiChatRequest chatRequest;

    private Prompt prompt;
}
