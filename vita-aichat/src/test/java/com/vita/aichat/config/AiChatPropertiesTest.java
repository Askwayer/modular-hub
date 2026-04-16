package com.vita.aichat.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatProperties;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class, AiChatConfiguration.class);

    @Test
    void shouldBindBusinessDefaultsAndDashScopeProviderProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.default-system-prompt=测试系统提示词",
                        "spring.ai.model.chat=dashscope",
                        "spring.ai.max.context.messages=24",
                        "spring.ai.stream.enabled=false",
                        "spring.ai.connect.timeout=3000",
                        "spring.ai.read.timeout=45000",
                        "spring.ai.dashscope.api-key=test-key",
                        "spring.ai.dashscope.base-url=https://dashscope.aliyuncs.com",
                        "spring.ai.dashscope.chat.options.model=qwen-plus"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AiChatProperties.class);
                    assertThat(context).hasSingleBean(org.springframework.ai.chat.client.ChatClient.class);
                    assertThat(context).hasSingleBean(ChatModel.class);

                    AiChatProperties properties = context.getBean(AiChatProperties.class);
                    DashScopeConnectionProperties connectionProperties = context.getBean(DashScopeConnectionProperties.class);
                    DashScopeChatProperties chatProperties = context.getBean(DashScopeChatProperties.class);
                    assertThat(properties.getDefaultSystemPrompt()).isEqualTo("测试系统提示词");
                    assertThat(properties.getMaxContextMessages()).isEqualTo(24);
                    assertThat(properties.getStreamEnabled()).isFalse();
                    assertThat(properties.getConnectTimeoutMs()).isEqualTo(3000L);
                    assertThat(properties.getReadTimeoutMs()).isEqualTo(45000L);
                    assertThat(connectionProperties.getApiKey()).isEqualTo("test-key");
                    assertThat(connectionProperties.getBaseUrl()).isEqualTo("https://dashscope.aliyuncs.com");
                    assertThat(chatProperties.getOptions().getModel()).isEqualTo("qwen-plus");
                });
    }

    @Test
    void shouldFailFastWhenApiKeyMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.default-system-prompt=测试系统提示词",
                        "spring.ai.model.chat=dashscope",
                        "spring.ai.max.context.messages=24",
                        "spring.ai.stream.enabled=true",
                        "spring.ai.connect.timeout=3000",
                        "spring.ai.read.timeout=45000",
                        "spring.ai.dashscope.base-url=https://dashscope.aliyuncs.com",
                        "spring.ai.dashscope.chat.options.model=qwen-plus"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("spring.ai.dashscope.api-key 不能为空");
                });
    }

    @Test
    void shouldFailFastWhenModelMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.default-system-prompt=测试系统提示词",
                        "spring.ai.model.chat=dashscope",
                        "spring.ai.max.context.messages=24",
                        "spring.ai.stream.enabled=true",
                        "spring.ai.connect.timeout=3000",
                        "spring.ai.read.timeout=45000",
                        "spring.ai.dashscope.api-key=test-key",
                        "spring.ai.dashscope.base-url=https://dashscope.aliyuncs.com"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("spring.ai.dashscope.chat.options.model 不能为空");
                });
    }

    @Test
    void shouldFailFastWhenBaseUrlMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.default-system-prompt=测试系统提示词",
                        "spring.ai.model.chat=dashscope",
                        "spring.ai.max.context.messages=24",
                        "spring.ai.stream.enabled=true",
                        "spring.ai.connect.timeout=3000",
                        "spring.ai.read.timeout=45000",
                        "spring.ai.dashscope.api-key=test-key",
                        "spring.ai.dashscope.chat.options.model=qwen-plus"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("spring.ai.dashscope.base-url 不能为空");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AiChatProperties.class, DashScopeConnectionProperties.class, DashScopeChatProperties.class})
    static class TestConfiguration {

        @Bean
        ChatModel chatModel() {
            return Mockito.mock(ChatModel.class);
        }
    }
}
