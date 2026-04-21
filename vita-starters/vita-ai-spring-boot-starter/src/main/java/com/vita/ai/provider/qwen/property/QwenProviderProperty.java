package com.vita.ai.provider.qwen.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.ai.providers.qwen")
public class QwenProviderProperty {

    private boolean enabled = false;

    private String baseUrl;

    private String apiKey;

    private String defaultModel;
}
