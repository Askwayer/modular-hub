package com.vita.aichat.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatProperties;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 收口 AI Chat 的最小自定义 Bean，模型调用仍以 Spring AI Alibaba 自动装配为主。
 */
@Configuration
@EnableConfigurationProperties(AiChatProperties.class)
public class AiChatConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiChatConfiguration.class);

    @Bean("aiChatClient")
    public ChatClient aiChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("aiChatExecutor")
    public Executor aiChatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-chat-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }

    @Bean
    public RestClientCustomizer aiChatRestClientCustomizer(AiChatProperties properties) {
        return builder -> builder.requestFactory(
                ClientHttpRequestFactoryBuilder.detect()
                        .build(ClientHttpRequestFactorySettings.defaults().withTimeouts(
                                Duration.ofMillis(properties.getConnectTimeoutMs()),
                                Duration.ofMillis(properties.getReadTimeoutMs())
                        ))
        );
    }

    @Bean
    public WebClientCustomizer aiChatWebClientCustomizer(AiChatProperties properties) {
        return builder -> builder.clientConnector(
                ClientHttpConnectorBuilder.detect()
                        .build(ClientHttpConnectorSettings.defaults().withTimeouts(
                                Duration.ofMillis(properties.getConnectTimeoutMs()),
                                Duration.ofMillis(properties.getReadTimeoutMs())
                        ))
        );
    }

    @Bean
    public InitializingBean aiChatProviderValidator(@Value("${spring.ai.model.chat:}") String provider,
                                                    Environment environment,
                                                    DashScopeConnectionProperties connectionProperties,
                                                    DashScopeChatProperties chatProperties) {
        return () -> {
            Assert.hasText(provider, "spring.ai.model.chat 不能为空");
            Assert.isTrue("dashscope".equalsIgnoreCase(provider), "spring.ai.model.chat 仅支持 dashscope");
            Assert.isTrue(environment.containsProperty("spring.ai.dashscope.api-key"), "spring.ai.dashscope.api-key 不能为空");
            Assert.isTrue(environment.containsProperty("spring.ai.dashscope.base-url"), "spring.ai.dashscope.base-url 不能为空");
            Assert.isTrue(environment.containsProperty("spring.ai.dashscope.chat.options.model"), "spring.ai.dashscope.chat.options.model 不能为空");
            Assert.hasText(connectionProperties.getApiKey(), "spring.ai.dashscope.api-key 不能为空");
            Assert.hasText(connectionProperties.getBaseUrl(), "spring.ai.dashscope.base-url 不能为空");
            Assert.notNull(chatProperties.getOptions(), "spring.ai.dashscope.chat.options 不能为空");
            Assert.hasText(chatProperties.getOptions().getModel(), "spring.ai.dashscope.chat.options.model 不能为空");

            log.info("Initialized DashScope chat provider: provider={}, baseUrl={}, model={}, apiKey={}",
                    provider,
                    connectionProperties.getBaseUrl(),
                    chatProperties.getOptions().getModel(),
                    mask(connectionProperties.getApiKey()));
        };
    }

    private String mask(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
