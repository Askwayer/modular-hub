package com.vita.aichat.chat.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会话详情响应。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiConversationDetailVo extends AiConversationPageVo {

    private String systemPrompt;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer maxContextMessages;
    private Integer summaryVersion;
}
