package com.vita.aichat.chat.support;

import com.vita.aichat.chat.property.AiChatProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;

/**
 * 在主对话事务提交后异步执行摘要与压缩。
 */
@Component
public class AiChatSummaryListener {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatSummaryListener.class);

    private final AiChatProperty aiChatProperty;
    private final Executor aiSummaryTaskExecutor;
    private final RoundSummaryService roundSummaryService;

    public AiChatSummaryListener(AiChatProperty aiChatProperty,
                                 @Qualifier("aiSummaryTaskExecutor") Executor aiSummaryTaskExecutor,
                                 RoundSummaryService roundSummaryService) {
        this.aiChatProperty = aiChatProperty;
        this.aiSummaryTaskExecutor = aiSummaryTaskExecutor;
        this.roundSummaryService = roundSummaryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoundSummaryEvent(AiChatRoundSummaryEvent event) {
        if (!Boolean.TRUE.equals(aiChatProperty.getSummary().getEnabled())
                || !Boolean.TRUE.equals(aiChatProperty.getSummary().getAsync())) {
            return;
        }
        aiSummaryTaskExecutor.execute(() -> {
            try {
                roundSummaryService.generateAndCompressIfNeeded(event);
            } catch (Exception ex) {
                LOG.warn("round summary async task failed, conversationId={}", event.getConversationId(), ex);
            }
        });
    }
}
