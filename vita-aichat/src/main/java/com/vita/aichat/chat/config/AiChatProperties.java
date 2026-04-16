package com.vita.aichat.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Chat 业务配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai")
public class AiChatProperties {

    private String defaultSystemPrompt = "你是一个严谨的开发助手。";

    private Model model = new Model();

    private Max max = new Max();

    private Stream stream = new Stream();

    private Connect connect = new Connect();

    private Read read = new Read();

    private Dashscope dashscope = new Dashscope();

    public String getProvider() {
        return model.getChat();
    }

    public Integer getMaxContextMessages() {
        return max.getContext().getMessages();
    }

    public Boolean getStreamEnabled() {
        return stream.getEnabled();
    }

    public Long getConnectTimeout() {
        return connect.getTimeout();
    }

    public Long getReadTimeout() {
        return read.getTimeout();
    }

    public String getDashscopeApiKey() {
        return dashscope.getApiKey();
    }

    public String getDashscopeBaseUrl() {
        return dashscope.getBaseUrl();
    }

    public String getDashscopeChatModel() {
        return dashscope.getChat().getOptions().getModel();
    }

    @Data
    public static class Model {
        private String chat = "dashscope";
    }

    @Data
    public static class Max {
        private Context context = new Context();

        @Data
        public static class Context {
            private Integer messages = 12;
        }
    }

    @Data
    public static class Stream {
        private Boolean enabled = Boolean.TRUE;
    }

    @Data
    public static class Connect {
        private Long timeout = 5000L;
    }

    @Data
    public static class Read {
        private Long timeout = 60000L;
    }

    @Data
    public static class Dashscope {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com";
        private Chat chat = new Chat();

        @Data
        public static class Chat {
            private Options options = new Options();

            @Data
            public static class Options {
                private String model = "qwen-plus";
            }
        }
    }
}
