package com.vita.aichat.chat.support;

import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.core.RedisCache;
import org.junit.jupiter.api.Test;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiChatRedisCoordinatorTest {

    @Test
    void shouldDeduplicateRequestIdUntilReleased() {
        RedisCache redisCache = mock(RedisCache.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        AiChatRedisCoordinator coordinator = new AiChatRedisCoordinator(redisCache, redissonClient, buildProperty());

        when(redisCache.setIfAbsent(anyString(), eq("req-1"), eq(24 * 60 * 60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true)
                .thenReturn(false);

        assertTrue(coordinator.markRequestIfAbsent(1001L, "req-1"));
        assertFalse(coordinator.markRequestIfAbsent(1001L, "req-1"));

        coordinator.releaseRequestMark(1001L, "req-1");

        verify(redisCache).deleteObject("aichat:req:1001:req-1");
    }

    @Test
    void shouldUseRedissonLockForConversationLocking() throws InterruptedException {
        RedisCache redisCache = mock(RedisCache.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        AiChatRedisCoordinator coordinator = new AiChatRedisCoordinator(redisCache, redissonClient, buildProperty());

        when(redissonClient.getLock("aichat:conv:2002:lock")).thenReturn(lock);
        when(lock.tryLock(0L, 600L, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertTrue(coordinator.acquireConversationLock(2002L));

        coordinator.releaseConversationLock(2002L);

        verify(lock).unlock();
    }

    @Test
    void shouldAcquireAndReleaseSemaphorePermits() throws InterruptedException {
        RedisCache redisCache = mock(RedisCache.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RPermitExpirableSemaphore activeSemaphore = mock(RPermitExpirableSemaphore.class);
        RPermitExpirableSemaphore waitingSemaphore = mock(RPermitExpirableSemaphore.class);
        RFuture<String> activeFuture = mock(RFuture.class);
        AiChatRedisCoordinator coordinator = new AiChatRedisCoordinator(redisCache, redissonClient, buildProperty());

        when(redissonClient.getPermitExpirableSemaphore("aichat:concurrency:active")).thenReturn(activeSemaphore);
        when(redissonClient.getPermitExpirableSemaphore("aichat:concurrency:waiting")).thenReturn(waitingSemaphore);
        when(activeSemaphore.tryAcquire(0L, 900L, TimeUnit.SECONDS)).thenReturn("active-1");
        when(waitingSemaphore.tryAcquire(0L, 35L, TimeUnit.SECONDS)).thenReturn("waiting-1");
        when(activeSemaphore.tryAcquire(30L, 900L, TimeUnit.SECONDS)).thenReturn("active-2");
        when(activeSemaphore.tryAcquireAsync(30L, 900L, TimeUnit.SECONDS)).thenReturn(activeFuture);

        assertEquals("active-1", coordinator.tryAcquireActivePermit());
        assertEquals("waiting-1", coordinator.tryAcquireWaitingPermit());
        assertEquals("active-2", coordinator.acquireActivePermit(30L));
        assertEquals(activeFuture, coordinator.acquireActivePermitAsync(30L));

        coordinator.releaseWaitingPermit("waiting-1");
        coordinator.releaseActivePermit("active-1");

        verify(waitingSemaphore).tryRelease("waiting-1");
        verify(activeSemaphore).tryRelease("active-1");
    }

    @Test
    void shouldInitializeConfiguredPermits() {
        RedisCache redisCache = mock(RedisCache.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RPermitExpirableSemaphore activeSemaphore = mock(RPermitExpirableSemaphore.class);
        RPermitExpirableSemaphore waitingSemaphore = mock(RPermitExpirableSemaphore.class);
        AiChatRedisCoordinator coordinator = new AiChatRedisCoordinator(redisCache, redissonClient, buildProperty());

        when(redissonClient.getPermitExpirableSemaphore("aichat:concurrency:active")).thenReturn(activeSemaphore);
        when(redissonClient.getPermitExpirableSemaphore("aichat:concurrency:waiting")).thenReturn(waitingSemaphore);
        when(redisCache.getCacheObject("aichat:concurrency:active:configured")).thenReturn(null);
        when(redisCache.getCacheObject("aichat:concurrency:waiting:configured")).thenReturn(null);
        when(activeSemaphore.getPermits()).thenReturn(0);
        when(waitingSemaphore.getPermits()).thenReturn(0);
        when(activeSemaphore.acquiredPermits()).thenReturn(0);
        when(waitingSemaphore.acquiredPermits()).thenReturn(0);

        coordinator.initializeConcurrencyPermits();

        verify(activeSemaphore).setPermits(5);
        verify(waitingSemaphore).setPermits(20);
        verify(redisCache).setCacheObject("aichat:concurrency:active:configured", 5);
        verify(redisCache).setCacheObject("aichat:concurrency:waiting:configured", 20);
    }

    private AiChatProperty buildProperty() {
        AiChatProperty property = new AiChatProperty();
        property.setConversationLockLeaseSeconds(600L);
        property.setConcurrencyWaitTimeoutSeconds(30L);
        property.setActivePermitLeaseSeconds(900L);
        property.setConcurrencyMaxActive(5);
        property.setConcurrencyMaxWaiting(20);
        return property;
    }
}
