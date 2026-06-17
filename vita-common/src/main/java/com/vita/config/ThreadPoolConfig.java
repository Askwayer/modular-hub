package com.vita.config;

import com.vita.utils.ThreadUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-03-13  17:20:25
 * @Description: 线程池配置类
 * @Version: 1.0
 */
@Configuration
public class ThreadPoolConfig {
    // 核心线程池大小
    private final int corePoolSize = 50;

    // 线程池维护线程所允许的空闲时间
    private int keepAliveSeconds = 300;

    // 日志线程池相关配置
    private static final int OPER_AUDIT_CORE_POOL_SIZE = 4;

    private static final int OPER_AUDIT_MAX_POOL_SIZE = 16;

    private static final int OPER_AUDIT_QUEUE_CAPACITY = 1000;

    private static final int LOGIN_AUDIT_CORE_POOL_SIZE = 2;

    private static final int LOGIN_AUDIT_MAX_POOL_SIZE = 8;

    private static final int LOGIN_AUDIT_QUEUE_CAPACITY = 500;

    private static final int GEO_ENRICH_CORE_POOL_SIZE = 2;

    private static final int GEO_ENRICH_MAX_POOL_SIZE = 4;

    private static final int GEO_ENRICH_QUEUE_CAPACITY = 200;

    /**
     * 线程池任务执行器
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = "threadPoolTaskExecutor")
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 最大可创建的线程数
        int maxPoolSize = 200;
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        // 队列最大长度
        int queueCapacity = 1000;
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("schedule-pool-" + count.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.DiscardPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                ThreadUtils.printException(r, t);
            }
        };
    }

    /**
     * 系统操作日志专用线程池。
     */
    @Bean(name = "operLogTaskExecutor")
    public ThreadPoolTaskExecutor operLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(OPER_AUDIT_CORE_POOL_SIZE);
        executor.setMaxPoolSize(OPER_AUDIT_MAX_POOL_SIZE);
        executor.setQueueCapacity(OPER_AUDIT_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("oper-log-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }

    /**
     * 登录审计与登录信息更新线程池。
     */
    @Bean(name = "loginAuditTaskExecutor")
    public ThreadPoolTaskExecutor loginAuditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(LOGIN_AUDIT_CORE_POOL_SIZE);
        executor.setMaxPoolSize(LOGIN_AUDIT_MAX_POOL_SIZE);
        executor.setQueueCapacity(LOGIN_AUDIT_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("login-audit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }

    /**
     * 登录归属地补全线程池。
     */
    @Bean(name = "geoEnrichTaskExecutor")
    public ThreadPoolTaskExecutor geoEnrichTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(GEO_ENRICH_CORE_POOL_SIZE);
        executor.setMaxPoolSize(GEO_ENRICH_MAX_POOL_SIZE);
        executor.setQueueCapacity(GEO_ENRICH_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("geo-enrich-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }

}
