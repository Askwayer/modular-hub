package com.vita.aichat.chat.support;

import com.vita.aichat.chat.model.context.AiChatCapacityPermit;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatConcurrencyCoordinatorTest {

    @Test
    void shouldAcquireActivePermitImmediatelyForSync() {
        AiChatRedisCoordinator redisCoordinator = mock(AiChatRedisCoordinator.class);
        AiChatConcurrencyCoordinator coordinator = new AiChatConcurrencyCoordinator(redisCoordinator, buildProperty());

        when(redisCoordinator.tryAcquireActivePermit()).thenReturn("active-1");

        AiChatCapacityPermit permit = coordinator.acquireCapacityForSync();

        assertEquals("active-1", permit.getActivePermitId());
        assertFalse(permit.isWaited());
        verify(redisCoordinator, never()).tryAcquireWaitingPermit();
    }

    @Test
    void shouldWaitForActivePermitAndReleaseWaitingSeatForSync() {
        AiChatRedisCoordinator redisCoordinator = mock(AiChatRedisCoordinator.class);
        AiChatConcurrencyCoordinator coordinator = new AiChatConcurrencyCoordinator(redisCoordinator, buildProperty());

        when(redisCoordinator.tryAcquireActivePermit()).thenReturn(null);
        when(redisCoordinator.tryAcquireWaitingPermit()).thenReturn("waiting-1");
        when(redisCoordinator.acquireActivePermit(30L)).thenReturn("active-2");

        AiChatCapacityPermit permit = coordinator.acquireCapacityForSync();

        assertEquals("active-2", permit.getActivePermitId());
        assertTrue(permit.isWaited());
        verify(redisCoordinator).releaseWaitingPermit("waiting-1");
    }

    @Test
    void shouldThrowQueueFullWhenWaitingSeatUnavailable() {
        AiChatRedisCoordinator redisCoordinator = mock(AiChatRedisCoordinator.class);
        AiChatConcurrencyCoordinator coordinator = new AiChatConcurrencyCoordinator(redisCoordinator, buildProperty());

        when(redisCoordinator.tryAcquireActivePermit()).thenReturn(null);
        when(redisCoordinator.tryAcquireWaitingPermit()).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, coordinator::acquireCapacityForSync);

        assertEquals(GlobalErrorCode.CHAT_CAPACITY_QUEUE_FULL.getCode(), exception.getCode());
    }

    @Test
    void shouldThrowWaitTimeoutWhenActivePermitNotAvailableInTime() {
        AiChatRedisCoordinator redisCoordinator = mock(AiChatRedisCoordinator.class);
        AiChatConcurrencyCoordinator coordinator = new AiChatConcurrencyCoordinator(redisCoordinator, buildProperty());

        when(redisCoordinator.tryAcquireActivePermit()).thenReturn(null);
        when(redisCoordinator.tryAcquireWaitingPermit()).thenReturn("waiting-1");
        when(redisCoordinator.acquireActivePermit(30L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, coordinator::acquireCapacityForSync);

        assertEquals(GlobalErrorCode.CHAT_CAPACITY_WAIT_TIMEOUT.getCode(), exception.getCode());
        verify(redisCoordinator).releaseWaitingPermit("waiting-1");
    }

    private AiChatProperty buildProperty() {
        AiChatProperty property = new AiChatProperty();
        property.setConcurrencyWaitTimeoutSeconds(30L);
        return property;
    }
}
