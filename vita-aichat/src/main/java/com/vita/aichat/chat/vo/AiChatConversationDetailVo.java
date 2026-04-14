package com.vita.aichat.chat.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会话详情响应。
 */
@Data
public class AiChatConversationDetailVo {

    private Long id;

    private String title;

    private String titleSource;

    private String status;

    private Byte pinned;

    private Integer messageCount;

    private Integer roundCount;

    private String currentModelCode;

    private String systemPrompt;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Integer maxContextMessages;

    private Integer summaryVersion;

    private String summaryStatus;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
