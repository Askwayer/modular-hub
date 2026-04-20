package com.vita.aichat.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息分页响应。
 */
@Data
public class AiMessagePageVo {

    private Long id;
    private String roleType;
    private String messageType;
    private Long replyToId;
    private String requestId;
    private String providerRequestId;
    private String providerCode;
    private String modelCode;
    private String content;
    private String reasoningContent;
    private String contentStatus;
    private String finishReason;
    private Byte useContext;
    private Integer tokenInput;
    private Integer tokenOutput;
    private Integer tokenTotal;
    private LocalDateTime firstTokenAt;
    private LocalDateTime completedAt;
    private Integer streamBatchCount;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
}
