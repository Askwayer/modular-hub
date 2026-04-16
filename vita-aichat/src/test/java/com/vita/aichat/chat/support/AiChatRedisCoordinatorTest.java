package com.vita.aichat.chat.support;

import com.vita.core.RedisCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatRedisCoordinatorTest {

    private final ObjectProvider<RedisCache> emptyProvider = new DefaultListableBeanFactory().getBeanProvider(RedisCache.class);

    @Test
    void shouldDeduplicateRequestIdUntilReleased() {
        AiChatRedisCoordinator coordinator = new AiChatRedisCoordinator(emptyProvider);

        assertTrue(coordinator.markRequestIfAbsent(1001L, "req-1"));
        assertFalse(coordinator.markRequestIfAbsent(1001L, "req-1"));

        coordinator.releaseRequestMark(1001L, "req-1");

        assertTrue(coordinator.markRequestIfAbsent(1001L, "req-1"));
    }

    @Test
    void shouldHoldConversationLockUntilReleased() {
        AiChatRedisCoordinator coordinator = new AiChatRedisCoordinator(emptyProvider);

        assertTrue(coordinator.acquireConversationLock(2002L, "req-1"));
        assertFalse(coordinator.acquireConversationLock(2002L, "req-2"));

        coordinator.releaseConversationLock(2002L);

        assertTrue(coordinator.acquireConversationLock(2002L, "req-3"));
    }
}
