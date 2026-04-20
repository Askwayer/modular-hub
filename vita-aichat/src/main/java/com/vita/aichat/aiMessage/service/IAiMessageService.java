package com.vita.aichat.aiMessage.service;

import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;

import java.util.List;

/**
 * 封装消息表在正式 Chat 场景下的读写操作。
 */
public interface IAiMessageService {

    void create(AiMessage message);

    void update(AiMessage message);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);

    PageResponse<AiMessage> pageByConversationIdAndUserId(PageRequest pageRequest, Long conversationId, Long userId);

    List<AiMessage> listRecentContextMessages(Long conversationId, Long userId, Long excludeMessageId, Integer limit);

    List<AiMessage> listPendingSummaryMessages(Long conversationId, Long userId, Long afterMessageId);

    AiMessage getByIdAndUserId(Long messageId, Long userId);

    List<AiMessage> listByConversationIdAndUserId(Long conversationId, Long userId);
}
