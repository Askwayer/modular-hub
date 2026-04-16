package com.vita.aichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 AI Chat 业务默认值和运行时开关。
 */
@Data
@ConfigurationProperties(prefix = "spring.ai")
public class AiChatProperties {

    /**
     * 默认系统提示词。
     */
    private String defaultSystemPrompt = "你是一个严谨的开发助手。";

    private Max max = new Max();

    private Stream stream = new Stream();

    private Timeout connect = new Timeout(5000L);

    private Timeout read = new Timeout(60000L);

    public Integer getMaxContextMessages() {
        return max.getContext().getMessages();
    }

    public Boolean getStreamEnabled() {
        return stream.getEnabled();
    }

    public Long getConnectTimeoutMs() {
        return connect.getTimeout();
    }

    public Long getReadTimeoutMs() {
        return read.getTimeout();
    }

    @Data
    public static class Max {

        private Context context = new Context();
    }

    @Data
    public static class Context {

        private Integer messages = 12;
    }

    @Data
    public static class Stream {

        private Boolean enabled = Boolean.TRUE;
    }

    @Data
    public static class Timeout {

        private Long timeout;

        public Timeout() {
        }

        public Timeout(Long timeout) {
            this.timeout = timeout;
        }
    }
}
