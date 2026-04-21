package com.vita.ai.provider.fastapi.config;

import com.vita.ai.provider.fastapi.FastApiProvider;
import com.vita.ai.provider.fastapi.property.FastApiProviderProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(FastApiProviderProperty.class)
@ConditionalOnProperty(prefix = "vita.ai.providers.fastapi", name = "enabled", havingValue = "true")
public class FastApiProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "fastApiAiWebClient")
    public WebClient fastApiAiWebClient(FastApiProviderProperty property) {
        WebClient.Builder builder = WebClient.builder().baseUrl(property.getBaseUrl());
        if (property.getApiKey() != null && !property.getApiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + property.getApiKey().trim());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(FastApiProvider.class)
    public FastApiProvider fastApiProvider(FastApiProviderProperty property, WebClient fastApiAiWebClient) {
        return new FastApiProvider(property, fastApiAiWebClient);
    }
}
