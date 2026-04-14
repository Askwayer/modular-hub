package com.vita.aichat.aiConversation.service;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;

/**
 * 封装会话表在正式 Chat 场景下的读写操作。
 */
public interface IAiConversationService {

    void create(AiConversation conversation);

    PageResponse<AiConversation> pageByUserId(PageRequest pageRequest, Long userId, String keyword);

    AiConversation getByIdAndUserId(Long conversationId, Long userId);

    void update(AiConversation conversation);

    void deleteById(Long conversationId);
}
