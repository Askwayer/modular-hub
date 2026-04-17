package com.vita.aichat.chat.support;

import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.core.RedisCache;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * AI Chat Redis 协调器。
 *
 * <p>负责 requestId 防重、会话分布式锁、活跃并发名额和等待队列席位。</p>
 */
@Component
public class AiChatRedisCoordinator {

    private static final long REQUEST_TTL_SECONDS = 24 * 60 * 60;
    private static final String ACTIVE_SEMAPHORE_KEY = "aichat:concurrency:active";
    private static final String WAITING_SEMAPHORE_KEY = "aichat:concurrency:waiting";
    private static final String ACTIVE_SEMAPHORE_CONFIG_KEY = "aichat:concurrency:active:configured";
    private static final String WAITING_SEMAPHORE_CONFIG_KEY = "aichat:concurrency:waiting:configured";
    private static final long WAITING_PERMIT_LEASE_BUFFER_SECONDS = 5L;

    private final RedisCache redisCache;
    private final RedissonClient redissonClient;
    private final AiChatProperty aiChatProperty;

    public AiChatRedisCoordinator(RedisCache redisCache,
                                  RedissonClient redissonClient,
                                  AiChatProperty aiChatProperty) {
        this.redisCache = redisCache;
        this.redissonClient = redissonClient;
        this.aiChatProperty = aiChatProperty;
    }

    /**
     * 标记请求并防重，若已存在则返回 false。
     */
    public boolean markRequestIfAbsent(Long conversationId, String requestId) {
        String cacheKey = buildRequestKey(conversationId, requestId);
        return redisCache.setIfAbsent(cacheKey, requestId, REQUEST_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 释放请求标记。
     */
    public void releaseRequestMark(Long conversationId, String requestId) {
        redisCache.deleteObject(buildRequestKey(conversationId, requestId));
    }

    /**
     * 获取会话锁，用于串行化同一会话的消息发送。
     */
    public boolean acquireConversationLock(Long conversationId) {
        try {
            return getConversationLock(conversationId).tryLock(
                    0L,
                    aiChatProperty.getLock().getConversation().getLeaseSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "获取会话锁时被中断");
        }
    }

    /**
     * 释放会话锁。
     */
    public void releaseConversationLock(Long conversationId) {
        RLock lock = getConversationLock(conversationId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 立即尝试获取活跃并发名额。
     */
    public String tryAcquireActivePermit() {
        try {
            return getActiveSemaphore().tryAcquire(0L, aiChatProperty.getConcurrency().getActivePermitLeaseSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "获取活跃生成名额时被中断");
        }
    }

    /**
     * 阻塞等待活跃并发名额。
     */
    public String acquireActivePermit(Long waitTimeoutSeconds) {
        try {
            return getActiveSemaphore().tryAcquire(
                    waitTimeoutSeconds,
                    aiChatProperty.getConcurrency().getActivePermitLeaseSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "等待活跃生成名额时被中断");
        }
    }

    /**
     * 异步等待活跃并发名额。
     */
    public RFuture<String> acquireActivePermitAsync(Long waitTimeoutSeconds) {
        return getActiveSemaphore().tryAcquireAsync(
                waitTimeoutSeconds,
                aiChatProperty.getConcurrency().getActivePermitLeaseSeconds(),
                TimeUnit.SECONDS
        );
    }

    /**
     * 立即尝试获取等待队列席位。
     */
    public String tryAcquireWaitingPermit() {
        try {
            return getWaitingSemaphore().tryAcquire(0L, waitingPermitLeaseSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "获取等待队列席位时被中断");
        }
    }

    /**
     * 释放活跃并发名额。
     */
    public void releaseActivePermit(String permitId) {
        if (permitId != null) {
            getActiveSemaphore().tryRelease(permitId);
        }
    }

    /**
     * 释放等待队列席位。
     */
    public void releaseWaitingPermit(String permitId) {
        if (permitId != null) {
            getWaitingSemaphore().tryRelease(permitId);
        }
    }

    /**
     * 按当前配置初始化并发名额。
     */
    public void initializeConcurrencyPermits() {
        initializeSemaphore(getActiveSemaphore(), ACTIVE_SEMAPHORE_CONFIG_KEY, aiChatProperty.getConcurrency().getMaxActive());
        initializeSemaphore(getWaitingSemaphore(), WAITING_SEMAPHORE_CONFIG_KEY, aiChatProperty.getConcurrency().getMaxWaiting());
    }

    /**
     * 构建会话锁的 Redis 键。
     */
    static String buildConversationLockKey(Long conversationId) {
        return "aichat:conv:" + conversationId + ":lock";
    }

    /**
     * 构建请求标记的 Redis 键。
     */
    static String buildRequestKey(Long conversationId, String requestId) {
        return "aichat:req:" + conversationId + ":" + requestId;
    }

    private long waitingPermitLeaseSeconds() {
        return aiChatProperty.getConcurrency().getWaitTimeoutSeconds() + WAITING_PERMIT_LEASE_BUFFER_SECONDS;
    }

    private void initializeSemaphore(RPermitExpirableSemaphore semaphore, String configKey, Integer configuredPermits) {
        Integer currentConfigured = redisCache.getCacheObject(configKey);
        int configured = configuredPermits == null ? 0 : configuredPermits;
        int totalPermits = semaphore.getPermits();
        if (Objects.equals(currentConfigured, configured) && totalPermits == configured) {
            return;
        }
        if (semaphore.acquiredPermits() > 0) {
            throw new ServiceException(
                    GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(),
                    "AI Chat 并发名额正在使用，暂时无法重置并发配置"
            );
        }
        semaphore.setPermits(configured);
        redisCache.setCacheObject(configKey, configured);
    }

    private RLock getConversationLock(Long conversationId) {
        return redissonClient.getLock(buildConversationLockKey(conversationId));
    }

    private RPermitExpirableSemaphore getActiveSemaphore() {
        return redissonClient.getPermitExpirableSemaphore(ACTIVE_SEMAPHORE_KEY);
    }

    private RPermitExpirableSemaphore getWaitingSemaphore() {
        return redissonClient.getPermitExpirableSemaphore(WAITING_SEMAPHORE_KEY);
    }
}
