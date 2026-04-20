package com.vita.ai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamChunk {

    private String requestId;

    private String providerRequestId;

    private String delta;

    private String reasoningDelta;

    private Boolean finished;

    private String finishReason;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;

    private String rawChunk;
}
