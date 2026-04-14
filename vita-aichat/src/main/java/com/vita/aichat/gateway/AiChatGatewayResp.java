package com.vita.aichat.gateway;

import lombok.Data;

/**
 * AI Chat 网关响应。
 */
@Data
public class AiChatGatewayResp {

    private String modelCode;

    private String content;

    private String reasoningContent;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;
}
