package com.vita.ai.provider.kimi.config;

import com.vita.ai.provider.kimi.KimiProvider;
import com.vita.ai.provider.kimi.property.KimiProviderProperty;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(KimiProviderProperty.class)
@ConditionalOnClass(OpenAiChatModel.class)
@ConditionalOnProperty(prefix = "spring.ai.providers.kimi", name = "enabled", havingValue = "true")
public class KimiProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "kimiChatModel")
    public OpenAiChatModel kimiChatModel(KimiProviderProperty property) {
        validate(property);
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(property.getBaseUrl().trim())
                .apiKey(property.getApiKey().trim())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(property.getDefaultModel().trim())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(KimiProvider.class)
    public KimiProvider kimiProvider(OpenAiChatModel kimiChatModel) {
        return new KimiProvider(kimiChatModel);
    }

    private void validate(KimiProviderProperty property) {
        Assert.hasText(property.getBaseUrl(), "spring.ai.providers.kimi.base-url must be set");
        Assert.hasText(property.getApiKey(), "spring.ai.providers.kimi.api-key must be set");
        Assert.hasText(property.getDefaultModel(), "spring.ai.providers.kimi.default-model must be set");
    }
}
