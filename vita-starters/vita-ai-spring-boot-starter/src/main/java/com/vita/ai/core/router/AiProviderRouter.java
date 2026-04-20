package com.vita.ai.core.router;

import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.provider.AiProvider;

public interface AiProviderRouter {

    AiProvider route(AiChatRequest request);
}
