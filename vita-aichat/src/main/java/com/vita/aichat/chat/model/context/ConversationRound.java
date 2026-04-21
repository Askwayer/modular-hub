package com.vita.aichat.chat.model.context;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiMessage.entity.AiMessage;
import lombok.Builder;
import lombok.Value;

/**
 * 一轮完整问答上下文。
 */
@Value
@Builder
public class ConversationRound {
    int roundNo;
    AiChatRequest chatRequest;
    AiMessage userMessage;
    AiMessage assistantMessage;
}
