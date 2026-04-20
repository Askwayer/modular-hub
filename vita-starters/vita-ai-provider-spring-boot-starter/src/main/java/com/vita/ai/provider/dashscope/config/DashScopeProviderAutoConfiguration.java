package com.vita.ai.provider.dashscope.config;

import com.vita.ai.provider.dashscope.DashScopeProvider;
import com.vita.ai.provider.dashscope.property.DashScopeProviderProperty;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DashScopeProviderProperty.class)
@ConditionalOnClass(ChatModel.class)
@ConditionalOnProperty(prefix = "spring.ai.providers.dashscope", name = "enabled", havingValue = "true")
public class DashScopeProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DashScopeProvider.class)
    public DashScopeProvider dashScopeProvider(ChatModel chatModel) {
        return new DashScopeProvider(chatModel);
    }
}
