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

    public boolean markRequestIfAbsent(Long conversationId, String requestId) {
        String cacheKey = buildRequestKey(conversationId, requestId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            return redisCache.setIfAbsent(cacheKey, requestId, REQUEST_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return setLocalIfAbsent(localRequestCache, cacheKey, REQUEST_TTL_SECONDS);
    }

    public boolean acquireConversationLock(Long conversationId, String requestId) {
        String cacheKey = buildConversationLockKey(conversationId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            return redisCache.setIfAbsent(cacheKey, requestId, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return setLocalIfAbsent(localLockCache, cacheKey, LOCK_TTL_SECONDS);
    }

    public void releaseConversationLock(Long conversationId) {
        String cacheKey = buildConversationLockKey(conversationId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            redisCache.deleteObject(cacheKey);
            return;
        }
        localLockCache.remove(cacheKey);
    }

    public void releaseRequestMark(Long conversationId, String requestId) {
        String cacheKey = buildRequestKey(conversationId, requestId);
        RedisCache redisCache = redisCacheProvider.getIfAvailable();
        if (redisCache != null) {
            redisCache.deleteObject(cacheKey);
            return;
        }
        localRequestCache.remove(cacheKey);
    }

    static String buildConversationLockKey(Long conversationId) {
        return "aichat:conv:" + conversationId + ":lock";
    }

    static String buildRequestKey(Long conversationId, String requestId) {
        return "aichat:req:" + conversationId + ":" + requestId;
    }

    private boolean setLocalIfAbsent(ConcurrentHashMap<String, Long> storage, String key, long ttlSeconds) {
        clearExpired(storage);
        long expireAt = Instant.now().plusSeconds(ttlSeconds).toEpochMilli();
        return storage.putIfAbsent(key, expireAt) == null;
    }

    private void clearExpired(ConcurrentHashMap<String, Long> storage) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : storage.entrySet()) {
            if (entry.getValue() != null && entry.getValue() <= now) {
                storage.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
