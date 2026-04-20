package com.vita.aichat.chat.support;

import com.vita.aichat.chat.model.context.PreparedSendContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 摘要异步事件分发器。
 */
@Component
public class AiChatSummaryDispatcher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public AiChatSummaryDispatcher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishRoundSummary(PreparedSendContext context) {
        applicationEventPublisher.publishEvent(new AiChatRoundSummaryEvent(
                context.getConversationId(),
                context.getUserId(),
                context.getUserMessage().getId(),
                context.getAssistantMessage().getId(),
                context.getProviderCode(),
                context.getModelCode()
        ));
    }
}
