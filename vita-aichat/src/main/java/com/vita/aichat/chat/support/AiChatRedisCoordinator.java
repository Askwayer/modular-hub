package com.vita.aichat.chat.support;

import com.vita.core.RedisCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AI Chat 轻量级 Redis 协调器。
 */
@Component
public class AiChatRedisCoordinator {

    private static final long REQUEST_TTL_SECONDS = 24 * 60 * 60;
    private static final long LOCK_TTL_SECONDS = 10 * 60;

    private final ObjectProvider<RedisCache> redisCacheProvider;

    private final ConcurrentHashMap<String, Long> localRequestCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> localLockCache = new ConcurrentHashMap<>();

    public AiChatRedisCoordinator(ObjectProvider<RedisCache> redisCacheProvider) {
        this.redisCacheProvider = redisCacheProvider;
    }

    /**
     * 标记请求并防重，若已存在则返回false。
     *
     * @param conversationId 会话ID
     * @param requestId 请求唯一标识
     * @return 是否标记成功
     */
    public boolean markRequestIfAbsent(Long conversationId, String requestId) {
        String cacheKey = buildRequestKey(conversationId, requestId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            return redisCache.setIfAbsent(cacheKey, requestId, REQUEST_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return setLocalIfAbsent(localRequestCache, cacheKey, REQUEST_TTL_SECONDS);
    }

    /**
     * 获取会话锁，用于串行化同一会话的消息发送。
     *
     * @param conversationId 会话ID
     * @param requestId 请求唯一标识
     * @return 是否获取锁成功
     */
    public boolean acquireConversationLock(Long conversationId, String requestId) {
        String cacheKey = buildConversationLockKey(conversationId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            return redisCache.setIfAbsent(cacheKey, requestId, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return setLocalIfAbsent(localLockCache, cacheKey, LOCK_TTL_SECONDS);
    }

    /**
     * 释放会话锁。
     *
     * @param conversationId 会话ID
     */
    public void releaseConversationLock(Long conversationId) {
        String cacheKey = buildConversationLockKey(conversationId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            redisCache.deleteObject(cacheKey);
            return;
        }
        localLockCache.remove(cacheKey);
    }

    /**
     * 释放请求标记。
     *
     * @param conversationId 会话ID
     * @param requestId 请求唯一标识
     */
    public void releaseRequestMark(Long conversationId, String requestId) {
        String cacheKey = buildRequestKey(conversationId, requestId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            redisCache.deleteObject(cacheKey);
            return;
        }
        localRequestCache.remove(cacheKey);
    }

    /**
     * 构建会话锁的Redis键。
     *
     * @param conversationId 会话ID
     * @return Redis键
     */
    static String buildConversationLockKey(Long conversationId) {
        return "aichat:conv:" + conversationId + ":lock";
    }

    /**
     * 构建请求标记的Redis键。
     *
     * @param conversationId 会话ID
     * @param requestId 请求唯一标识
     * @return Redis键
     */
    static String buildRequestKey(Long conversationId, String requestId) {
        return "aichat:req:" + conversationId + ":" + requestId;
    }

    /**
     * 在本地缓存中设置键值，若不存在则插入。
     *
     * @param storage 本地并发存储
     * @param key 缓存键
     * @param ttlSeconds 过期时间（秒）
     * @return 是否设置成功
     */
    private boolean setLocalIfAbsent(ConcurrentHashMap<String, Long> storage, String key, long ttlSeconds) {
        clearExpired(storage);
        long expireAt = Instant.now().plusSeconds(ttlSeconds).toEpochMilli();
        return storage.putIfAbsent(key, expireAt) == null;
    }

    /**
     * 清理本地缓存中已过期的条目。
     *
     * @param storage 本地并发存储
     */
    private void clearExpired(ConcurrentHashMap<String, Long> storage) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : storage.entrySet()) {
            if (entry.getValue() != null && entry.getValue() <= now) {
                storage.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
