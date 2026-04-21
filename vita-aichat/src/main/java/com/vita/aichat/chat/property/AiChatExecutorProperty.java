package com.vita.aichat.chat.property;

import lombok.Data;

@Data
public class AiChatExecutorProperty {

    private Integer corePoolSize = 8;

    private Integer maxPoolSize = 16;

    private Integer queueCapacity = 0;

    private Integer keepAliveSeconds = 60;
}
