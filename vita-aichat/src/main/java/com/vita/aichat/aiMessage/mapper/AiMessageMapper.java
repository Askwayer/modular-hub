package com.vita.aichat.aiMessage.mapper;

import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.core.mapper.BaseMapperX;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

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
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getUseContext, (byte) 1)
                .eq(AiMessage::getContentStatus, "DONE")
                .neIfPresent(AiMessage::getId, excludeMessageId)
                .orderByDesc(AiMessage::getCreateTime)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + limit);
        return selectList(queryWrapper);
    }
}
