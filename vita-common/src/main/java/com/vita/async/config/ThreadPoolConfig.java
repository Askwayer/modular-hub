package com.vita.async.config;

import com.vita.async.property.AsyncExecutorProperty;
import com.vita.async.property.DefaultAsyncExecutorProperty;
import com.vita.async.property.HotRefreshAsyncExecutorProperty;
import com.vita.async.property.MarketRefreshAsyncExecutorProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.async.config
 * @Author: znk
 * @CreateTime: 2026-03-13  17:20:25
 * @Description: 线程池配置类
 * @Version: 1.0
 */
@Configuration
public class ThreadPoolConfig {
    private static final DefaultAsyncExecutorProperty DEFAULT_EXECUTOR_PROPERTY = new DefaultAsyncExecutorProperty();
    private static final HotRefreshAsyncExecutorProperty HOT_REFRESH_EXECUTOR_PROPERTY =
            new HotRefreshAsyncExecutorProperty();
    private static final MarketRefreshAsyncExecutorProperty MARKET_REFRESH_EXECUTOR_PROPERTY =
            new MarketRefreshAsyncExecutorProperty();

    /**
     * 非核心异步默认线程池，同时保留旧别名避免 Bean 名立即断裂。
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = {"defaultTaskExecutor", "threadPoolTaskExecutor"})
    @Primary
    public ThreadPoolTaskExecutor defaultTaskExecutor() {
        return createExecutor(DEFAULT_EXECUTOR_PROPERTY, new ThreadPoolExecutor.DiscardPolicy());
    }

    /**
     * 热榜全量刷新专用线程池。
     */
    @Bean(name = "hotRefreshTaskExecutor")
    public ThreadPoolTaskExecutor hotRefreshTaskExecutor() {
        return createExecutor(HOT_REFRESH_EXECUTOR_PROPERTY, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 行情模块刷新专用受限线程池。
     *
     * @return 行情刷新执行器
     */
    @Bean(name = "marketRefreshTaskExecutor")
    public ThreadPoolTaskExecutor marketRefreshTaskExecutor() {
        return createExecutor(MARKET_REFRESH_EXECUTOR_PROPERTY, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 创建线程池执行器实例。
     *
     * @param property 线程池配置对象
     * @param rejectedExecutionHandler 拒绝策略处理器，当线程池无法接受新任务时执行
     * @return 配置完成的 ThreadPoolTaskExecutor 实例
     */
    private ThreadPoolTaskExecutor createExecutor(AsyncExecutorProperty property,
                                                  RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(property.getCorePoolSize());
        executor.setMaxPoolSize(property.getMaxPoolSize());
        executor.setQueueCapacity(property.getQueueCapacity());
        executor.setKeepAliveSeconds(property.getKeepAliveSeconds());
        executor.setThreadNamePrefix(property.getThreadNamePrefix());
        // 配置优雅关闭：等待任务完成并设置终止超时时间
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(property.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        return executor;
    }
}
