package com.vita.aichat.chat.support;

import com.vita.ai.core.exception.AiProviderNotFoundException;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.property.AiCoreProperty;
import com.vita.ai.core.property.AiModuleProperty;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AichatModuleStrategyTest {

    @Test
    void shouldDefaultProviderToQwen() {
        AiCoreProperty property = new AiCoreProperty();
        AichatModuleStrategy strategy = new AichatModuleStrategy(property);

        AiChatRequest request = strategy.prepare(AiChatRequest.builder().module("aichat").build());

        assertThat(request.getProvider()).isEqualTo("qwen");
        assertThat(request.getModel()).isEqualTo("qwen-plus");
    }

    @Test
    void shouldRejectDashscopeWhenAllowedProvidersAreQwenAndKimi() {
        AiModuleProperty moduleProperty = new AiModuleProperty();
        moduleProperty.setAllowedProviders(List.of("qwen", "kimi"));

        AiCoreProperty property = new AiCoreProperty();
        property.getModules().put("aichat", moduleProperty);

        AichatModuleStrategy strategy = new AichatModuleStrategy(property);

        assertThatThrownBy(() -> strategy.prepare(AiChatRequest.builder()
                        .module("aichat")
                        .provider("dashscope")
                        .build()))
                .isInstanceOf(AiProviderNotFoundException.class)
                .hasMessageContaining("dashscope");
    }
}
