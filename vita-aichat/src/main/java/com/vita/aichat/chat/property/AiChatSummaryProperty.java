package com.vita.aichat.chat.property;

import lombok.Data;

@Data
public class AiChatSummaryProperty {

    private Boolean enabled = true;

    private Boolean async = true;

    private String roundSummaryProvider;

    private String roundSummaryModelCode;

    private Boolean compressEnabled = true;

    private Integer compressThresholdCount = 10;

    private Integer compressThresholdTokens = 12000;

    private String compressModelProvider;

    private String compressModelCode;

    private AiChatExecutorProperty executor = new AiChatExecutorProperty();
}
