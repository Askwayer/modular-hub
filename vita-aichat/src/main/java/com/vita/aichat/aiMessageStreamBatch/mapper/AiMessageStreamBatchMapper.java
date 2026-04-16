package com.vita.aichat.aiMessageStreamBatch.mapper;

import com.vita.aichat.aiMessageStreamBatch.entity.AiMessageStreamBatch;
import com.vita.core.mapper.BaseMapperX;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * AI 流式批次快照表 Mapper。
 */
@Mapper
public interface AiMessageStreamBatchMapper extends BaseMapperX<AiMessageStreamBatch> {

    default List<AiMessageStreamBatch> selectListByAssistantMessageId(Long assistantMessageId) {
        return selectList(new LambdaQueryWrapperX<AiMessageStreamBatch>()
                .eq(AiMessageStreamBatch::getAssistantMessageId, assistantMessageId)
                .orderByAsc(AiMessageStreamBatch::getBatchNo)
                .orderByAsc(AiMessageStreamBatch::getId));
    }

    default void deleteByAssistantMessageIds(Collection<Long> assistantMessageIds) {
        if (assistantMessageIds == null || assistantMessageIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<AiMessageStreamBatch>()
                .in(AiMessageStreamBatch::getAssistantMessageId, assistantMessageIds));
    }
}
