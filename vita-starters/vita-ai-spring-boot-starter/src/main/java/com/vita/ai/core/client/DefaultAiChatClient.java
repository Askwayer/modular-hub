package com.vita.ai.core.client;

import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import com.vita.ai.core.provider.AiProvider;
import com.vita.ai.core.router.AiProviderRouter;
import com.vita.ai.core.strategy.AiModuleStrategy;
import com.vita.ai.core.strategy.AiModuleStrategyRegistry;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class DefaultAiChatClient implements AiChatClient {

    private final AiModuleStrategyRegistry strategyRegistry;
    private final AiProviderRouter aiProviderRouter;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        AiModuleStrategy strategy = strategyRegistry.get(request.getModule());
        AiChatRequest preparedRequest = strategy.prepare(request);
        AiProvider provider = aiProviderRouter.route(preparedRequest);
        return provider.chat(preparedRequest);
    }

    @Override
    public Flux<AiStreamChunk> streamChat(AiChatRequest request) {
        AiModuleStrategy strategy = strategyRegistry.get(request.getModule());
        AiChatRequest preparedRequest = strategy.prepare(request);
        AiProvider provider = aiProviderRouter.route(preparedRequest);
        return provider.streamChat(preparedRequest);
    }
}
