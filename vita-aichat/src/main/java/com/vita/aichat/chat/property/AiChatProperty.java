package com.vita.aichat.chat.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Chat 业务配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai")
public class AiChatProperty {

    /**
     * 模型提供者。
     */
    private String modelChat = "dashscope";

    /**
     * 最大上下文消息数。
     */
    private Integer maxContextMessages = 12;

    /**
     * 是否启用流式响应。
     */
    private Boolean streamEnabled = true;

    /**
     * 连接超时时间（毫秒）。
     */
    private Long connectTimeout = 5000L;

    /**
     * 读取超时时间（毫秒）。
     */
    private Long readTimeout = 60000L;

    /**
     * 默认系统提示词。
     */
    private String defaultSystemPrompt = "你是一个严谨的开发助手。";

    private Lock lock = new Lock();
    private Concurrency concurrency = new Concurrency();
    private Executor executor = new Executor();
    private Dashscope dashscope = new Dashscope();

    @Data
    public static class Lock {
        private Conversation conversation = new Conversation();

        @Data
        public static class Conversation {
            private Long leaseSeconds = 600L;
        }
    }

    @Data
    public static class Concurrency {
        private Integer maxActive = 5;
        private Integer maxWaiting = 20;
        private Long waitTimeoutSeconds = 30L;
        private Long activePermitLeaseSeconds = 900L;
    }

    @Data
    public static class Executor {
        private Integer corePoolSize = 8;
        private Integer maxPoolSize = 16;
        private Integer queueCapacity = 0;
        private Integer keepAliveSeconds = 60;
    }

    @Data
    public static class Dashscope {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com";
        private ChatOptions chat = new ChatOptions();

        @Data
        public static class ChatOptions {
            private String model = "qwen-plus";
        }
    }
}