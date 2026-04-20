package com.vita.ai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String requestId;

    private String provider;

    private String providerRequestId;

    private String model;

    private String content;

    private String reasoningContent;

    private String finishReason;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;

    private String rawResponse;
}
