package com.vita.aichat.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话分页响应。
 */
@Data
public class AiChatConversationPageVo {

    private Long id;

    private String title;

    private String titleSource;

    private String status;

    private Byte pinned;

    private Integer messageCount;

    private Integer roundCount;

    private String currentModelCode;

    private String summaryStatus;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createTime;
}
