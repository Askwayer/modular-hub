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

    /**
     * 会话锁租期（秒）。
     */
    @Value("${spring.ai.lock.conversation.lease-seconds:600}")
    private Long conversationLockLeaseSeconds;

    /**
     * 全局活跃生成并发数。
     */
    @Value("${spring.ai.concurrency.max-active:5}")
    private Integer concurrencyMaxActive;

    /**
     * 等待队列容量。
     */
    @Value("${spring.ai.concurrency.max-waiting:20}")
    private Integer concurrencyMaxWaiting;

    /**
     * 等待活跃名额超时时间（秒）。
     */
    @Value("${spring.ai.concurrency.wait-timeout-seconds:30}")
    private Long concurrencyWaitTimeoutSeconds;

    /**
     * 活跃名额租期（秒）。
     */
    @Value("${spring.ai.concurrency.active-permit-lease-seconds:900}")
    private Long activePermitLeaseSeconds;

    /**
     * AI Chat 独立线程池核心线程数。
     */
    @Value("${spring.ai.executor.core-pool-size:8}")
    private Integer executorCorePoolSize;

    /**
     * AI Chat 独立线程池最大线程数。
     */
    @Value("${spring.ai.executor.max-pool-size:16}")
    private Integer executorMaxPoolSize;

    /**
     * AI Chat 独立线程池队列容量。
     */
    @Value("${spring.ai.executor.queue-capacity:0}")
    private Integer executorQueueCapacity;

    /**
     * AI Chat 独立线程池空闲线程存活时间（秒）。
     */
    @Value("${spring.ai.executor.keep-alive-seconds:60}")
    private Integer executorKeepAliveSeconds;
}
