package com.vita.ai.core.provider;

import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import reactor.core.publisher.Flux;

public interface AiProvider {

    String providerCode();

    default boolean supports(String targetProviderCode) {
        return providerCode() != null && providerCode().equalsIgnoreCase(targetProviderCode);
    }

    AiChatResponse chat(AiChatRequest request);

    Flux<AiStreamChunk> streamChat(AiChatRequest request);
}
