package com.vita.aichat.aiChatRequest.mapper;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.core.mapper.BaseMapperX;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * AI 请求生命周期表 Mapper。
 */
@Mapper
public interface AiChatRequestMapper extends BaseMapperX<AiChatRequest> {

    default AiChatRequest selectByConversationIdAndRequestId(Long conversationId, String requestId) {
        return selectOne(new LambdaQueryWrapperX<AiChatRequest>()
                .eq(AiChatRequest::getConversationId, conversationId)
                .eq(AiChatRequest::getRequestId, requestId));
    }

    default List<AiChatRequest> selectListByConversationIdAndUserId(Long conversationId, Long userId) {
        return selectList(new LambdaQueryWrapperX<AiChatRequest>()
                .eq(AiChatRequest::getConversationId, conversationId)
                .eq(AiChatRequest::getUserId, userId));
    }

    default List<AiChatRequest> selectListByAssistantMessageIds(Collection<Long> assistantMessageIds) {
        if (assistantMessageIds == null || assistantMessageIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<AiChatRequest>()
                .in(AiChatRequest::getAssistantMessageId, assistantMessageIds));
    }
}
