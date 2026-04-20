package com.vita.aichat.aiConversationSummary.mapper;

import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.core.mapper.BaseMapperX;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.mapper
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface AiConversationSummaryMapper extends BaseMapperX<AiConversationSummary> {

    default AiConversationSummary selectLatestReadySummary(Long conversationId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AiConversationSummary>()
                .eq(AiConversationSummary::getConversationId, conversationId)
                .eq(AiConversationSummary::getUserId, userId)
                .eq(AiConversationSummary::getStatus, "READY")
                .orderByDesc(AiConversationSummary::getVersionNo)
                .last("LIMIT 1"));
    }

    default AiConversationSummary selectLatestSummary(Long conversationId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AiConversationSummary>()
                .eq(AiConversationSummary::getConversationId, conversationId)
                .eq(AiConversationSummary::getUserId, userId)
                .orderByDesc(AiConversationSummary::getVersionNo)
                .last("LIMIT 1"));
    }

    @Update("""
            UPDATE ai_conversation_summary
               SET status = #{targetStatus}
             WHERE conversation_id = #{conversationId}
               AND user_id = #{userId}
               AND status = #{currentStatus}
               AND is_deleted = 0
            """)
    int updateStatusByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                              @Param("userId") Long userId,
                                              @Param("currentStatus") String currentStatus,
                                              @Param("targetStatus") String targetStatus);
}
