package com.vita.aichat.aiConversationSummary.service;

import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;

/**
 * 封装摘要表在正式 Chat 场景下的读写操作。
 */
public interface IAiConversationSummaryService {

    AiConversationSummary getLatestReadySummary(Long conversationId, Long userId);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
