package com.vita.ai.provider.dashscope.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.ai.providers.dashscope")
public class DashScopeProviderProperty {

    private boolean enabled = true;

    private String baseUrl;

    private String apiKey;

    private String defaultModel;
}
