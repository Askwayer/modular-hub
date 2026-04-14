package com.vita.aichat.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 为正式 Chat 链路提供线程池和 Spring AI 客户端超时配置。
 */
@Configuration
public class AiChatConfiguration {

    private final AiChatProperties aiChatProperties;

    public AiChatConfiguration(AiChatProperties aiChatProperties) {
        this.aiChatProperties = aiChatProperties;
    }

    @Bean(name = "aiChatExecutor")
    public Executor aiChatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-chat-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }

    @Primary
    @Bean
    public RestClient.Builder aiChatRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(aiChatProperties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Math.toIntExact(aiChatProperties.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Primary
    @Bean
    public WebClient.Builder aiChatWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(aiChatProperties.getConnectTimeoutMs()))
                .responseTimeout(Duration.ofMillis(aiChatProperties.getReadTimeoutMs()));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
