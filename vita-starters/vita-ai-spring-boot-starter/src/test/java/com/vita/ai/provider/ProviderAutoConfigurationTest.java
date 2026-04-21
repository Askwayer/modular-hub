package com.vita.ai.provider;

import com.vita.ai.provider.kimi.KimiProvider;
import com.vita.ai.provider.kimi.config.KimiProviderAutoConfiguration;
import com.vita.ai.provider.qwen.QwenProvider;
import com.vita.ai.provider.qwen.config.QwenProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    QwenProviderAutoConfiguration.class,
                    KimiProviderAutoConfiguration.class
            ));

    @Test
    void shouldLoadQwenProviderOnly() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.providers.qwen.enabled=true",
                        "spring.ai.providers.qwen.base-url=https://dashscope.aliyuncs.com",
                        "spring.ai.providers.qwen.api-key=test-qwen-key",
                        "spring.ai.providers.qwen.default-model=qwen-plus"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(QwenProvider.class);
                    assertThat(context).doesNotHaveBean(KimiProvider.class);
                    assertThat(context).hasBean("qwenChatModel");
                    assertThat(context.getBean(QwenProvider.class).providerCode()).isEqualTo("qwen");
                });
    }

    @Test
    void shouldLoadKimiProviderOnly() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.providers.kimi.enabled=true",
                        "spring.ai.providers.kimi.base-url=https://api.moonshot.cn/v1",
                        "spring.ai.providers.kimi.api-key=test-kimi-key",
                        "spring.ai.providers.kimi.default-model=kimi-k2.5"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(QwenProvider.class);
                    assertThat(context).hasSingleBean(KimiProvider.class);
                    assertThat(context).hasBean("kimiChatModel");
                });
    }

    @Test
    void shouldLoadQwenAndKimiProvidersTogether() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.providers.qwen.enabled=true",
                        "spring.ai.providers.qwen.base-url=https://dashscope.aliyuncs.com",
                        "spring.ai.providers.qwen.api-key=test-qwen-key",
                        "spring.ai.providers.qwen.default-model=qwen-plus",
                        "spring.ai.providers.kimi.enabled=true",
                        "spring.ai.providers.kimi.base-url=https://api.moonshot.cn/v1",
                        "spring.ai.providers.kimi.api-key=test-kimi-key",
                        "spring.ai.providers.kimi.default-model=kimi-k2.5"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(QwenProvider.class);
                    assertThat(context).hasSingleBean(KimiProvider.class);
                    assertThat(context).hasBean("qwenChatModel");
                    assertThat(context).hasBean("kimiChatModel");
                });
    }

    @Test
    void shouldSkipProviderBeansWhenDisabledWithoutKeys() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.providers.qwen.enabled=false",
                        "spring.ai.providers.kimi.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(QwenProvider.class);
                    assertThat(context).doesNotHaveBean(KimiProvider.class);
                    assertThat(context).doesNotHaveBean("qwenChatModel");
                    assertThat(context).doesNotHaveBean("kimiChatModel");
                });
    }
}
