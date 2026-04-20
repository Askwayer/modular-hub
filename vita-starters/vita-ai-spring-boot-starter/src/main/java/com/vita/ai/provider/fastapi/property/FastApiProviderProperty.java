package com.vita.ai.provider.fastapi.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "vita.ai.providers.fastapi")
public class FastApiProviderProperty {

    private boolean enabled = false;

    private String baseUrl = "http://127.0.0.1:8000";

    private String apiKey;

    private String chatPath = "/chat";

    private String streamPath = "/stream-chat";

    private String defaultModel = "demo-chat-model";
}
