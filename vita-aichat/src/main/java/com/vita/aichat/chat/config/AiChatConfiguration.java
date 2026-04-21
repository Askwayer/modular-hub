package com.vita.aichat.chat.config;

import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.chat.support.AiChatConcurrencyCoordinator;
import com.vita.core.RedisCache;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI Chat 配置。
 */
@Configuration
public class AiChatConfiguration {

    private final AiChatProperty aiChatProperty;
    private final AiChatConcurrencyCoordinator aiChatConcurrencyCoordinator;
    private final ObjectProvider<RedisCache> redisCacheProvider;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public AiChatConfiguration(AiChatProperty aiChatProperty,
                               AiChatConcurrencyCoordinator aiChatConcurrencyCoordinator,
                               ObjectProvider<RedisCache> redisCacheProvider,
                               ObjectProvider<RedissonClient> redissonClientProvider) {
        this.aiChatProperty = aiChatProperty;
        this.aiChatConcurrencyCoordinator = aiChatConcurrencyCoordinator;
        this.redisCacheProvider = redisCacheProvider;
        this.redissonClientProvider = redissonClientProvider;
    }

    /**
     * 配置 AI Chat 异步执行器。
     */
    @Bean(name = {"aiChatTaskExecutor", "aiChatExecutor"})
    public ThreadPoolTaskExecutor aiChatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(aiChatProperty.getExecutor().getCorePoolSize());
        executor.setMaxPoolSize(aiChatProperty.getExecutor().getMaxPoolSize());
        executor.setQueueCapacity(aiChatProperty.getExecutor().getQueueCapacity());
        executor.setKeepAliveSeconds(aiChatProperty.getExecutor().getKeepAliveSeconds());
        executor.setThreadNamePrefix("ai-chat-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    /**
     * 配置摘要与压缩异步执行器。
     */
    @Bean(name = "aiSummaryTaskExecutor")
    public ThreadPoolTaskExecutor aiSummaryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(aiChatProperty.getSummary().getExecutor().getCorePoolSize());
        executor.setMaxPoolSize(aiChatProperty.getSummary().getExecutor().getMaxPoolSize());
        executor.setQueueCapacity(aiChatProperty.getSummary().getExecutor().getQueueCapacity());
        executor.setKeepAliveSeconds(aiChatProperty.getSummary().getExecutor().getKeepAliveSeconds());
        executor.setThreadNamePrefix("ai-summary-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    /**
     * 配置 AI Chat 启动校验器，验证必要配置项。
     *
     * @return 初始化回调
     */
    @Bean
    public InitializingBean aiChatStartupValidator() {
        return () -> {
            if (redisCacheProvider.getIfAvailable() == null || redissonClientProvider.getIfAvailable() == null) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "AI Chat 依赖 Redis 与 Redisson，请检查相关配置");
            }
            if (aiChatProperty.getLock().getConversation().getLeaseSeconds() == null || aiChatProperty.getLock().getConversation().getLeaseSeconds() <= 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.lock.conversation.lease-seconds 必须大于 0");
            }
            if (aiChatProperty.getConcurrency().getMaxActive() == null || aiChatProperty.getConcurrency().getMaxActive() <= 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.concurrency.max-active 必须大于 0");
            }
            if (aiChatProperty.getConcurrency().getMaxWaiting() == null || aiChatProperty.getConcurrency().getMaxWaiting() < 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.concurrency.max-waiting 不能小于 0");
            }
            if (aiChatProperty.getConcurrency().getWaitTimeoutSeconds() == null || aiChatProperty.getConcurrency().getWaitTimeoutSeconds() <= 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.concurrency.wait-timeout-seconds 必须大于 0");
            }
            if (aiChatProperty.getConcurrency().getActivePermitLeaseSeconds() == null || aiChatProperty.getConcurrency().getActivePermitLeaseSeconds() <= 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.concurrency.active-permit-lease-seconds 必须大于 0");
            }
            if (aiChatProperty.getExecutor().getCorePoolSize() == null || aiChatProperty.getExecutor().getCorePoolSize() <= 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.executor.core-pool-size 必须大于 0");
            }
            if (aiChatProperty.getExecutor().getMaxPoolSize() == null
                    || aiChatProperty.getExecutor().getMaxPoolSize() < aiChatProperty.getConcurrency().getMaxActive()) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.executor.max-pool-size 必须大于等于 vita.aichat.concurrency.max-active");
            }
            if (aiChatProperty.getExecutor().getMaxPoolSize() < aiChatProperty.getExecutor().getCorePoolSize()) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.executor.max-pool-size 不能小于 vita.aichat.executor.core-pool-size");
            }
            if (aiChatProperty.getExecutor().getQueueCapacity() == null || aiChatProperty.getExecutor().getQueueCapacity() < 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.executor.queue-capacity 不能小于 0");
            }
            if (Boolean.TRUE.equals(aiChatProperty.getSummary().getEnabled())) {
                if (aiChatProperty.getSummary().getCompressThresholdCount() == null || aiChatProperty.getSummary().getCompressThresholdCount() <= 0) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.summary.compress-threshold-count 必须大于 0");
                }
                if (aiChatProperty.getSummary().getExecutor().getCorePoolSize() == null || aiChatProperty.getSummary().getExecutor().getCorePoolSize() <= 0) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "vita.aichat.summary.executor.core-pool-size 必须大于 0");
                }
            }
            aiChatConcurrencyCoordinator.initializePermits();
        };
    }
}
