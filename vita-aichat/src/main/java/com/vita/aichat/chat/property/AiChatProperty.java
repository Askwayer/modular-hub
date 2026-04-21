package com.vita.aichat.chat.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Chat 业务配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "vita.aichat")
public class AiChatProperty {

    /**
     * 最大上下文消息数。
     */
    private Integer maxContextMessages = 12;

    /**
     * 是否启用流式响应。
     */
    private Boolean streamEnabled = true;

    /**
     * 默认系统提示词。
     */
    private String defaultSystemPrompt = "你是一个严谨的开发助手。";

    private AiChatLockProperty lock = new AiChatLockProperty();

    private AiChatConcurrencyProperty concurrency = new AiChatConcurrencyProperty();

    private AiChatExecutorProperty executor = new AiChatExecutorProperty();

    private AiChatContextProperty context = new AiChatContextProperty();

    private AiChatMemoryProperty memory = new AiChatMemoryProperty();

    private AiChatSummaryProperty summary = new AiChatSummaryProperty();
}
