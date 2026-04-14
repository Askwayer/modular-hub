package com.vita.aichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 收敛正式 Chat 业务默认值和模型请求超时配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "vita.aichat")
public class AiChatProperties {

    private String defaultSystemPrompt = "你是一个严谨的开发助手。";
    private String defaultModelCode = "qwen-plus";
    private Integer maxContextMessages = 12;
    private Boolean streamEnabled = true;
    private Long connectTimeoutMs = 5000L;
    private Long readTimeoutMs = 60000L;
}
