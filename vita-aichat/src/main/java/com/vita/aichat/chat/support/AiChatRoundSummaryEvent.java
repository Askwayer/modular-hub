package com.vita.aichat.chat.support;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 主对话成功后触发的轮次摘要事件。
 */
@Data
@AllArgsConstructor
public class AiChatRoundSummaryEvent {

    private Long conversationId;

    private Long userId;

    private Long userMessageId;

    private Long assistantMessageId;

    private String providerCode;

    private String modelCode;
}
