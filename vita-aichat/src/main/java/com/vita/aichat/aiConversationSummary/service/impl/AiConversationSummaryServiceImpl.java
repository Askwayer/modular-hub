package com.vita.aichat.aiConversationSummary.service.impl;

import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.mapper.AiConversationSummaryMapper;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;

/**
 * 摘要数据访问封装，供正式 Chat 业务调用。
 */
@Service
public class AiConversationSummaryServiceImpl implements IAiConversationSummaryService {

    private final AiConversationSummaryMapper aiConversationSummaryMapper;

    public AiConversationSummaryServiceImpl(AiConversationSummaryMapper aiConversationSummaryMapper) {
        this.aiConversationSummaryMapper = aiConversationSummaryMapper;
    }

    @Override
    public AiConversationSummary getLatestReadySummary(Long conversationId, Long userId) {
        return aiConversationSummaryMapper.selectLatestReadySummary(conversationId, userId);
    }

    @Override
    public void deleteByConversationIdAndUserId(Long conversationId, Long userId) {
        aiConversationSummaryMapper.delete(new LambdaQueryWrapperX<AiConversationSummary>()
                .eq(AiConversationSummary::getConversationId, conversationId)
                .eq(AiConversationSummary::getUserId, userId));
    }
}
