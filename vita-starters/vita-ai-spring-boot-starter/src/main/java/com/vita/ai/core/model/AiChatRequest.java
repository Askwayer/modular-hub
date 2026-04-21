package com.vita.ai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    private String requestId;

    private String module;

    private Long conversationId;

    private Long userId;

    private String provider;

    private String model;

    private String systemPrompt;

    private String userPrompt;

    private List<AiChatMessage> messages;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Boolean stream;

    private Map<String, Object> options;
}
