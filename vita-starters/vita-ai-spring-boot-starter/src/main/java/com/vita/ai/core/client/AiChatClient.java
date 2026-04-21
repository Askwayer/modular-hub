package com.vita.ai.core.client;

import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import reactor.core.publisher.Flux;

public interface AiChatClient {

    AiChatResponse chat(AiChatRequest request);

    Flux<AiStreamChunk> streamChat(AiChatRequest request);
}
