package com.vita.aichat.gateway;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Chat 网关请求。
 */
@Data
public class AiChatGatewayReq {

    private String modelCode;

    private String systemPrompt;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Long userId;

    private List<AiChatGatewayMessage> messages = new ArrayList<>();
}
