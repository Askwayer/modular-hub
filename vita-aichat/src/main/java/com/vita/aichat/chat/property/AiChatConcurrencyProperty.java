package com.vita.aichat.chat.property;

import lombok.Data;

@Data
public class AiChatConcurrencyProperty {

    private Integer maxActive = 5;

    private Integer maxWaiting = 20;

    private Long waitTimeoutSeconds = 30L;

    private Long activePermitLeaseSeconds = 900L;
}
