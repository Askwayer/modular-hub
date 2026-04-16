package com.vita.aichat.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSE 事件内容。
 */
@Data
public class AiChatStreamEventVo {

    private Long conversationId;
    private Long assistantMessageId;
    private String requestId;
    private Integer batchNo;
    private String finishReason;
    private String content;
    private String reasoningContent;
    private String contentStatus;
    private LocalDateTime firstTokenAt;
    private LocalDateTime completedAt;
    private Integer tokenInput;
    private Integer tokenOutput;
    private Integer tokenTotal;
}
