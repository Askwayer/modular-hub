package com.vita.aichat.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步发送消息响应。
 */
@Data
public class AiChatSendVo {

    private Long conversationId;
    private String requestId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String providerCode;
    private String modelCode;
    private String content;
    private String reasoningContent;
    private String contentStatus;
    private String finishReason;
    private Integer tokenInput;
    private Integer tokenOutput;
    private Integer tokenTotal;
    private LocalDateTime firstTokenAt;
    private LocalDateTime completedAt;
    private Integer streamBatchCount;
}
