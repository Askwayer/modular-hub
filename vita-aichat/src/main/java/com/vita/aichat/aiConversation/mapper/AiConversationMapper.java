package com.vita.aichat.aiConversation.mapper;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.core.mapper.BaseMapperX;
import com.vita.core.page.PageRequest;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.mapper
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface AiConversationMapper extends BaseMapperX<AiConversation> {

    default AiConversation selectByIdAndUserId(Long id, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AiConversation>()
                .eq(AiConversation::getId, id)
                .eq(AiConversation::getUserId, userId));
    }

    default PageResponse<AiConversation> selectPageByUserId(PageRequest pageRequest, Long userId, String keyword) {
        LambdaQueryWrapperX<AiConversation> queryWrapper = new LambdaQueryWrapperX<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .likeIfPresent(AiConversation::getTitle, keyword)
                .orderByDesc(AiConversation::getLastMessageAt)
                .orderByDesc(AiConversation::getId);
        return selectPage(pageRequest, queryWrapper);
    }
}
