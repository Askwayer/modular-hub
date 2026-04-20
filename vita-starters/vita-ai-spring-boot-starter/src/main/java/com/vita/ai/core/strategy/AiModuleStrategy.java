package com.vita.ai.core.strategy;

import com.vita.ai.core.model.AiChatRequest;

public interface AiModuleStrategy {

    String module();

    default boolean supports(String targetModule) {
        return module() != null && module().equalsIgnoreCase(targetModule);
    }

    AiChatRequest prepare(AiChatRequest request);
}
