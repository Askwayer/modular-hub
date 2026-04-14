package com.vita.aichat.aiConversation.service.impl;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.mapper.AiConversationMapper;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;
import org.springframework.stereotype.Service;

/**
 * 会话数据访问封装，供正式 Chat 业务调用。
 */
@Service
public class AiConversationServiceImpl implements IAiConversationService {

    private final AiConversationMapper aiConversationMapper;

    public AiConversationServiceImpl(AiConversationMapper aiConversationMapper) {
        this.aiConversationMapper = aiConversationMapper;
    }

    @Override
    public void create(AiConversation conversation) {
        aiConversationMapper.insert(conversation);
    }

    @Override
    public PageResponse<AiConversation> pageByUserId(PageRequest pageRequest, Long userId, String keyword) {
        return aiConversationMapper.selectPageByUserId(pageRequest, userId, keyword);
    }

    @Override
    public AiConversation getByIdAndUserId(Long conversationId, Long userId) {
        return aiConversationMapper.selectByIdAndUserId(conversationId, userId);
    }

    @Override
    public void update(AiConversation conversation) {
        aiConversationMapper.updateById(conversation);
    }

    @Override
    public void deleteById(Long conversationId) {
        aiConversationMapper.deleteById(conversationId);
    }
}
