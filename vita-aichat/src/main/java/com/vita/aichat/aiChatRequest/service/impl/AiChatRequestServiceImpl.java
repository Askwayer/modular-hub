package com.vita.aichat.aiChatRequest.service.impl;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.mapper.AiChatRequestMapper;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 请求生命周期表读写服务实现。
 */
@Service
public class AiChatRequestServiceImpl implements IAiChatRequestService {

    private final AiChatRequestMapper aiChatRequestMapper;

    public AiChatRequestServiceImpl(AiChatRequestMapper aiChatRequestMapper) {
        this.aiChatRequestMapper = aiChatRequestMapper;
    }

    @Override
    public void create(AiChatRequest request) {
        aiChatRequestMapper.insert(request);
    }

    @Override
    public void update(AiChatRequest request) {
        aiChatRequestMapper.updateById(request);
    }

    @Override
    public AiChatRequest getByConversationIdAndRequestId(Long conversationId, String requestId) {
        return aiChatRequestMapper.selectByConversationIdAndRequestId(conversationId, requestId);
    }

    @Override
    public List<AiChatRequest> listByConversationIdAndUserId(Long conversationId, Long userId) {
        return aiChatRequestMapper.selectListByConversationIdAndUserId(conversationId, userId);
    }

    @Override
    public List<AiChatRequest> listRecentSuccessfulChatRequests(Long conversationId, Long userId, Integer limit) {
        return aiChatRequestMapper.selectRecentSuccessfulChatRequests(conversationId, userId, limit);
    }

    @Override
    public List<AiChatRequest> listByAssistantMessageIds(Collection<Long> assistantMessageIds) {
        return aiChatRequestMapper.selectListByAssistantMessageIds(assistantMessageIds);
    }

    @Override
    public void deleteByConversationIdAndUserId(Long conversationId, Long userId) {
        aiChatRequestMapper.delete(new LambdaQueryWrapperX<AiChatRequest>()
                .eq(AiChatRequest::getConversationId, conversationId)
                .eq(AiChatRequest::getUserId, userId));
    }
}
