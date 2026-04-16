package com.vita.aichat.chat.support;

import com.vita.aichat.chat.model.context.AiChatCapacityPermit;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.redisson.api.RFuture;
import org.springframework.stereotype.Component;

/**
 * AI Chat 并发治理协调器。
 *
 * <p>负责全局活跃并发与等待队列的获取、释放和异常语义转换。</p>
 */
@Component
public class AiChatConcurrencyCoordinator {

    private final AiChatRedisCoordinator aiChatRedisCoordinator;
    private final AiChatProperty aiChatProperty;

    public AiChatConcurrencyCoordinator(AiChatRedisCoordinator aiChatRedisCoordinator,
                                        AiChatProperty aiChatProperty) {
        this.aiChatRedisCoordinator = aiChatRedisCoordinator;
        this.aiChatProperty = aiChatProperty;
    }

    /**
     * 同步接口获取活跃并发名额。
     */
    public AiChatCapacityPermit acquireCapacityForSync() {
        String activePermitId = aiChatRedisCoordinator.tryAcquireActivePermit();
        if (activePermitId != null) {
            return new AiChatCapacityPermit(activePermitId, false);
        }

        String waitingPermitId = aiChatRedisCoordinator.tryAcquireWaitingPermit();
        if (waitingPermitId == null) {
            throw new ServiceException(GlobalErrorCode.CHAT_CAPACITY_QUEUE_FULL);
        }

        try {
            activePermitId = aiChatRedisCoordinator.acquireActivePermit(aiChatProperty.getConcurrencyWaitTimeoutSeconds());
            if (activePermitId == null) {
                throw new ServiceException(GlobalErrorCode.CHAT_CAPACITY_WAIT_TIMEOUT);
            }
            return new AiChatCapacityPermit(activePermitId, true);
        } finally {
            aiChatRedisCoordinator.releaseWaitingPermit(waitingPermitId);
        }
    }

    /**
     * 流式接口立即尝试获取活跃并发名额。
     */
    public AiChatCapacityPermit tryAcquireCapacityForStream() {
        String activePermitId = aiChatRedisCoordinator.tryAcquireActivePermit();
        if (activePermitId == null) {
            return null;
        }
        return new AiChatCapacityPermit(activePermitId, false);
    }

    /**
     * 流式接口尝试进入等待队列。
     */
    public String tryAcquireWaitingPermit() {
        String waitingPermitId = aiChatRedisCoordinator.tryAcquireWaitingPermit();
        if (waitingPermitId == null) {
            throw new ServiceException(GlobalErrorCode.CHAT_CAPACITY_QUEUE_FULL);
        }
        return waitingPermitId;
    }

    /**
     * 异步等待活跃并发名额。
     */
    public RFuture<String> acquireActivePermitAsync() {
        return aiChatRedisCoordinator.acquireActivePermitAsync(aiChatProperty.getConcurrencyWaitTimeoutSeconds());
    }

    /**
     * 释放活跃并发名额。
     */
    public void releaseActivePermit(String activePermitId) {
        aiChatRedisCoordinator.releaseActivePermit(activePermitId);
    }

    /**
     * 释放等待队列席位。
     */
    public void releaseWaitingPermit(String waitingPermitId) {
        aiChatRedisCoordinator.releaseWaitingPermit(waitingPermitId);
    }

    /**
     * 获取会话锁。
     */
    public boolean acquireConversationLock(Long conversationId) {
        return aiChatRedisCoordinator.acquireConversationLock(conversationId);
    }

    /**
     * 释放会话锁。
     */
    public void releaseConversationLock(Long conversationId) {
        aiChatRedisCoordinator.releaseConversationLock(conversationId);
    }

    /**
     * 初始化并发名额。
     */
    public void initializePermits() {
        aiChatRedisCoordinator.initializeConcurrencyPermits();
    }
}
