package com.vita.aichat.aiMessage.mapper;

import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.core.mapper.BaseMapperX;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiMessage.mapper
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface AiMessageMapper extends BaseMapperX<AiMessage> {

    default PageResponse<AiMessage> selectPageByConversationIdAndUserId(PageRequest pageRequest, Long conversationId, Long userId) {
        LambdaQueryWrapperX<AiMessage> queryWrapper = new LambdaQueryWrapperX<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId);
        queryWrapper.orderByAsc(AiMessage::getCreateTime);
        queryWrapper.orderByAsc(AiMessage::getId);
        return selectPage(pageRequest, queryWrapper);
    }

    default List<AiMessage> selectRecentContextMessages(Long conversationId, Long userId, Long excludeMessageId, Integer limit) {
        if (limit == null || limit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapperX<AiMessage> queryWrapper = new LambdaQueryWrapperX<AiMessage>()
                .eqIfPresent(AiMessage::getConversationId, conversationId)
                .eqIfPresent(AiMessage::getUserId, userId)
                .eqIfPresent(AiMessage::getUseContext, (byte) 1)
                .eqIfPresent(AiMessage::getContentStatus, "DONE")
                .neIfPresent(AiMessage::getRoleType, "SUMMARY")
                .neIfPresent(AiMessage::getId, excludeMessageId)
                .orderByDesc(AiMessage::getCreateTime)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + limit);
        return selectList(queryWrapper);
    }

    default List<AiMessage> selectListByConversationIdAndUserId(Long conversationId, Long userId) {
        return selectList(new LambdaQueryWrapperX<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .orderByAsc(AiMessage::getCreateTime)
                .orderByAsc(AiMessage::getId));
    }

    default AiMessage selectByIdAndUserId(Long messageId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AiMessage>()
                .eq(AiMessage::getId, messageId)
                .eq(AiMessage::getUserId, userId));
    }

    default List<AiMessage> selectPendingSummaryMessages(Long conversationId, Long userId, Long afterMessageId) {
        return selectList(new LambdaQueryWrapperX<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getRoleType, "SUMMARY")
                .eq(AiMessage::getContentStatus, "DONE")
                .gtIfPresent(AiMessage::getId, afterMessageId)
                .orderByAsc(AiMessage::getCreateTime)
                .orderByAsc(AiMessage::getId));
    }

    default List<AiMessage> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<AiMessage>().in(AiMessage::getId, ids));
    }
}
