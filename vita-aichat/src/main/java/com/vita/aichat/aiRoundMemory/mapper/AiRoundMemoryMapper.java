package com.vita.aichat.aiRoundMemory.mapper;

import com.vita.aichat.aiRoundMemory.entity.AiRoundMemory;
import com.vita.core.mapper.BaseMapperX;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * AI 会话轮次记忆 Mapper。
 */
@Mapper
public interface AiRoundMemoryMapper extends BaseMapperX<AiRoundMemory> {

    default AiRoundMemory selectLatestRoundMemory(Long conversationId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AiRoundMemory>()
                .eq(AiRoundMemory::getConversationId, conversationId)
                .eq(AiRoundMemory::getUserId, userId)
                .orderByDesc(AiRoundMemory::getRoundNo)
                .last("LIMIT 1"));
    }

    default List<AiRoundMemory> selectReadyCarryForwardMemories(Long conversationId, Long userId) {
        return selectList(new LambdaQueryWrapperX<AiRoundMemory>()
                .eq(AiRoundMemory::getConversationId, conversationId)
                .eq(AiRoundMemory::getUserId, userId)
                .eq(AiRoundMemory::getStatus, "READY")
                .eq(AiRoundMemory::getCarryForward, (byte) 1)
                .orderByAsc(AiRoundMemory::getRoundNo));
    }

    default List<AiRoundMemory> selectReadyRollupCandidates(Long conversationId, Long userId) {
        return selectList(new LambdaQueryWrapperX<AiRoundMemory>()
                .eq(AiRoundMemory::getConversationId, conversationId)
                .eq(AiRoundMemory::getUserId, userId)
                .eq(AiRoundMemory::getStatus, "READY")
                .orderByAsc(AiRoundMemory::getRoundNo));
    }

    default List<AiRoundMemory> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<AiRoundMemory>().in(AiRoundMemory::getId, ids));
    }
}
