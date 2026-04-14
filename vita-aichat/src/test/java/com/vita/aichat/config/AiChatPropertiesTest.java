package com.vita.aichat.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(AiChatProperties.class);

    @Test
    void shouldBindAiChatPropertiesAndExposeSpringAiEnvironmentProperties() {
        contextRunner
                .withPropertyValues(
                        "vita.aichat.default-system-prompt=测试系统提示词",
                        "vita.aichat.default-model-code=qwen-turbo",
                        "vita.aichat.max-context-messages=24",
                        "vita.aichat.stream-enabled=false",
                        "vita.aichat.connect-timeout-ms=3000",
                        "vita.aichat.read-timeout-ms=45000",
                        "spring.ai.openai.api-key=test-key",
                        "spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "spring.ai.openai.chat.options.model=qwen-turbo"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AiChatProperties.class);
                    AiChatProperties properties = context.getBean(AiChatProperties.class);
                    assertThat(properties.getDefaultSystemPrompt()).isEqualTo("测试系统提示词");
                    assertThat(properties.getDefaultModelCode()).isEqualTo("qwen-turbo");
                    assertThat(properties.getMaxContextMessages()).isEqualTo(24);
                    assertThat(properties.getStreamEnabled()).isFalse();
                    assertThat(properties.getConnectTimeoutMs()).isEqualTo(3000L);
                    assertThat(properties.getReadTimeoutMs()).isEqualTo(45000L);
                    assertThat(context.getEnvironment().getProperty("spring.ai.openai.api-key")).isEqualTo("test-key");
                    assertThat(context.getEnvironment().getProperty("spring.ai.openai.base-url"))
                            .isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
                    assertThat(context.getEnvironment().getProperty("spring.ai.openai.chat.options.model"))
                            .isEqualTo("qwen-turbo");
                });
    }
}
