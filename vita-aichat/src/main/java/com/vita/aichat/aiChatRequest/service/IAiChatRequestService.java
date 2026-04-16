package com.vita.aichat.aiChatRequest.service;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;

import java.util.Collection;
import java.util.List;

/**
 * 请求生命周期表读写服务。
 */
public interface IAiChatRequestService {

    void create(AiChatRequest request);

    void update(AiChatRequest request);

    AiChatRequest getByConversationIdAndRequestId(Long conversationId, String requestId);

    List<AiChatRequest> listByConversationIdAndUserId(Long conversationId, Long userId);

    List<AiChatRequest> listByAssistantMessageIds(Collection<Long> assistantMessageIds);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
