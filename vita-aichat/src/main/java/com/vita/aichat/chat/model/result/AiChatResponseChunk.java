package com.vita.aichat.chat.model.result;

import lombok.Data;

/**
 * 单个模型响应片段。
 */
@Data
public class AiChatResponseChunk {

    private String providerRequestId;

    private String contentDelta;

    private String reasoningDelta;

    private String finishReason;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;
}
