package com.vita.ai.core.router;

import com.vita.ai.core.exception.AiProviderNotFoundException;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import com.vita.ai.core.provider.AiProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAiProviderRouterTest {

    @Test
    void shouldRouteQwenAndKimiProviders() {
        AiProvider qwenProvider = new StubProvider("qwen");
        AiProvider kimiProvider = new StubProvider("kimi");
        DefaultAiProviderRouter router = new DefaultAiProviderRouter(List.of(qwenProvider, kimiProvider));

        assertThat(router.route(AiChatRequest.builder().provider("qwen").build())).isSameAs(qwenProvider);
        assertThat(router.route(AiChatRequest.builder().provider("kimi").build())).isSameAs(kimiProvider);
    }

    @Test
    void shouldRejectDashscopeProviderCode() {
        DefaultAiProviderRouter router = new DefaultAiProviderRouter(List.of(
                new StubProvider("qwen"),
                new StubProvider("kimi")
        ));

        assertThatThrownBy(() -> router.route(AiChatRequest.builder().provider("dashscope").build()))
                .isInstanceOf(AiProviderNotFoundException.class)
                .hasMessageContaining("dashscope");
    }

    private static final class StubProvider implements AiProvider {

        private final String providerCode;

        private StubProvider(String providerCode) {
            this.providerCode = providerCode;
        }

        @Override
        public String providerCode() {
            return providerCode;
        }

        @Override
        public AiChatResponse chat(AiChatRequest request) {
            return null;
        }

        @Override
        public Flux<AiStreamChunk> streamChat(AiChatRequest request) {
            return Flux.empty();
        }
    }
}
