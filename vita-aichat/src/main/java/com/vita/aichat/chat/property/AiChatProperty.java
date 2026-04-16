package com.vita.aichat.chat.property;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI Chat 业务配置。
 */
@Data
@Component
public class AiChatProperty {

    /**
     * 模型提供者。
     */
    @Value("${spring.ai.model.chat:dashscope}")
    private String modelChat;

    /**
     * 最大上下文消息数。
     */
    @Value("${spring.ai.max.context.messages:12}")
    private Integer maxContextMessages;

    /**
     * 是否启用流式响应。
     */
    @Value("${spring.ai.stream.enabled:true}")
    private Boolean streamEnabled;

    /**
     * 连接超时时间（毫秒）。
     */
    @Value("${spring.ai.connect.timeout:5000}")
    private Long connectTimeout;

    /**
     * 读取超时时间（毫秒）。
     */
    @Value("${spring.ai.read.timeout:60000}")
    private Long readTimeout;

    /**
     * DashScope API Key。
     */
    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    /**
     * DashScope 基础 URL。
     */
    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com}")
    private String dashscopeBaseUrl;

    /**
     * DashScope 聊天模型名称。
     */
    @Value("${spring.ai.dashscope.chat.options.model:qwen-plus}")
    private String dashscopeChatModel;

    /**
     * 默认系统提示词。
     */
    @Value("${spring.ai.default-system-prompt:你是一个严谨的开发助手。}")
    private String defaultSystemPrompt;
}
