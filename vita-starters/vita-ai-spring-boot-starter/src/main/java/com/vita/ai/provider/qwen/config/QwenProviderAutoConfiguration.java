package com.vita.ai.provider.qwen.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.vita.ai.provider.qwen.QwenProvider;
import com.vita.ai.provider.qwen.property.QwenProviderProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(QwenProviderProperty.class)
@ConditionalOnClass(DashScopeChatModel.class)
@ConditionalOnProperty(prefix = "spring.ai.providers.qwen", name = "enabled", havingValue = "true")
public class QwenProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "qwenChatModel")
    public DashScopeChatModel qwenChatModel(QwenProviderProperty property) {
        validate(property);
        DashScopeApi api = DashScopeApi.builder()
                .baseUrl(property.getBaseUrl().trim())
                .apiKey(property.getApiKey().trim())
                .build();
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(property.getDefaultModel().trim())
                .build();
        return DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(QwenProvider.class)
    public QwenProvider qwenProvider(DashScopeChatModel qwenChatModel) {
        return new QwenProvider(qwenChatModel);
    }

    private void validate(QwenProviderProperty property) {
        Assert.hasText(property.getBaseUrl(), "spring.ai.providers.qwen.base-url must be set");
        Assert.hasText(property.getApiKey(), "spring.ai.providers.qwen.api-key must be set");
        Assert.hasText(property.getDefaultModel(), "spring.ai.providers.qwen.default-model must be set");
    }
}
