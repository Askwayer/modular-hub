package com.vita.aichat.aiMessage.service.impl;

import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.mapper.AiMessageMapper;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息数据访问封装，供正式 Chat 业务调用。
 */
@Service
public class AiMessageServiceImpl implements IAiMessageService {

    private final AiMessageMapper aiMessageMapper;

    public AiMessageServiceImpl(AiMessageMapper aiMessageMapper) {
        this.aiMessageMapper = aiMessageMapper;
    }

    @Override
    public void create(AiMessage message) {
        aiMessageMapper.insert(message);
    }

    @Override
    public void update(AiMessage message) {
        aiMessageMapper.updateById(message);
    }

    @Override
    public void deleteByConversationIdAndUserId(Long conversationId, Long userId) {
        aiMessageMapper.delete(new LambdaQueryWrapperX<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId));
    }

    @Override
    public PageResponse<AiMessage> pageByConversationIdAndUserId(PageRequest pageRequest, Long conversationId, Long userId) {
        return aiMessageMapper.selectPageByConversationIdAndUserId(pageRequest, conversationId, userId);
    }

    @Override
    public List<AiMessage> listRecentContextMessages(Long conversationId, Long userId, Long excludeMessageId, Integer limit) {
        return aiMessageMapper.selectRecentContextMessages(conversationId, userId, excludeMessageId, limit);
    }

    @Override
    public List<AiMessage> listPendingSummaryMessages(Long conversationId, Long userId, Long afterMessageId) {
        return aiMessageMapper.selectPendingSummaryMessages(conversationId, userId, afterMessageId);
    }

    @Override
    public AiMessage getByIdAndUserId(Long messageId, Long userId) {
        return aiMessageMapper.selectByIdAndUserId(messageId, userId);
    }

    @Override
    public List<AiMessage> listByConversationIdAndUserId(Long conversationId, Long userId) {
        return aiMessageMapper.selectListByConversationIdAndUserId(conversationId, userId);
    }
}
