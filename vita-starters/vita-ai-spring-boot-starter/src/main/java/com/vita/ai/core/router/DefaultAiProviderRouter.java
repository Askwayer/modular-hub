package com.vita.ai.core.router;

import com.vita.ai.core.exception.AiProviderNotFoundException;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.provider.AiProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class DefaultAiProviderRouter implements AiProviderRouter {

    private final List<AiProvider> providers;

    @Override
    public AiProvider route(AiChatRequest request) {
        String providerCode = request.getProvider();
        return providers.stream()
                .filter(provider -> provider.supports(providerCode))
                .findFirst()
                .orElseThrow(() -> new AiProviderNotFoundException(providerCode));
    }
}
