package com.vita.aichat.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话分页响应。
 */
@Data
public class AiConversationPageVo {

    private Long id;
    private String title;
    private String titleSource;
    private String status;
    private Byte pinned;
    private LocalDateTime lastMessageAt;
    private Integer messageCount;
    private Integer roundCount;
    private String currentModelCode;
    private String summaryStatus;
    private String activeRequestId;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime createTime;
}
