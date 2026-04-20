package com.vita.aichat.chat.property;

import com.vita.ai.core.property.AiCoreProperty;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatConfigurationBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "vita.ai.modules.aichat.default-provider=qwen",
                    "vita.ai.modules.aichat.default-model=qwen-plus",
                    "vita.ai.modules.aichat.allowed-providers[0]=qwen",
                    "vita.ai.modules.aichat.allowed-providers[1]=kimi",
                    "vita.aichat.default-system-prompt=system prompt",
                    "vita.aichat.max-context-messages=24",
                    "vita.aichat.stream-enabled=false",
                    "vita.aichat.lock.conversation.lease-seconds=120",
                    "vita.aichat.concurrency.max-active=7",
                    "vita.aichat.summary.round-summary-provider=kimi",
                    "vita.aichat.summary.round-summary-model-code=kimi-k2.5",
                    "vita.aichat.summary.compress-model-provider=qwen",
                    "vita.aichat.summary.compress-model-code=qwen-plus"
            );

    @Test
    void shouldBindAiCoreAndAiChatProperties() {
        contextRunner.run(context -> {
            AiCoreProperty aiCoreProperty = context.getBean(AiCoreProperty.class);
            AiChatProperty aiChatProperty = context.getBean(AiChatProperty.class);

            assertThat(aiCoreProperty.getModules().get("aichat").getDefaultProvider()).isEqualTo("qwen");
            assertThat(aiCoreProperty.getModules().get("aichat").getDefaultModel()).isEqualTo("qwen-plus");
            assertThat(aiCoreProperty.getModules().get("aichat").getAllowedProviders()).containsExactly("qwen", "kimi");

            assertThat(aiChatProperty.getDefaultSystemPrompt()).isEqualTo("system prompt");
            assertThat(aiChatProperty.getMaxContextMessages()).isEqualTo(24);
            assertThat(aiChatProperty.getStreamEnabled()).isFalse();
            assertThat(aiChatProperty.getLock().getConversation().getLeaseSeconds()).isEqualTo(120L);
            assertThat(aiChatProperty.getConcurrency().getMaxActive()).isEqualTo(7);
            assertThat(aiChatProperty.getSummary().getRoundSummaryProvider()).isEqualTo("kimi");
            assertThat(aiChatProperty.getSummary().getRoundSummaryModelCode()).isEqualTo("kimi-k2.5");
            assertThat(aiChatProperty.getSummary().getCompressModelProvider()).isEqualTo("qwen");
            assertThat(aiChatProperty.getSummary().getCompressModelCode()).isEqualTo("qwen-plus");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AiCoreProperty.class, AiChatProperty.class})
    static class TestConfiguration {
    }
}
