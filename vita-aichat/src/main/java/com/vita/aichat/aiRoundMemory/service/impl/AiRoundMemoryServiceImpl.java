package com.vita.aichat.aiRoundMemory.service.impl;

import com.vita.aichat.aiRoundMemory.entity.AiRoundMemory;
import com.vita.aichat.aiRoundMemory.mapper.AiRoundMemoryMapper;
import com.vita.aichat.aiRoundMemory.service.IAiRoundMemoryService;
import com.vita.mybatis.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 会话轮次记忆服务实现。
 */
@Service
public class AiRoundMemoryServiceImpl implements IAiRoundMemoryService {

    private final AiRoundMemoryMapper aiRoundMemoryMapper;

    public AiRoundMemoryServiceImpl(AiRoundMemoryMapper aiRoundMemoryMapper) {
        this.aiRoundMemoryMapper = aiRoundMemoryMapper;
    }

    @Override
    public AiRoundMemory getLatestRoundMemory(Long conversationId, Long userId) {
        return aiRoundMemoryMapper.selectLatestRoundMemory(conversationId, userId);
    }

    @Override
    public List<AiRoundMemory> listReadyCarryForwardMemories(Long conversationId, Long userId) {
        return aiRoundMemoryMapper.selectReadyCarryForwardMemories(conversationId, userId);
    }

    @Override
    public List<AiRoundMemory> listReadyRollupCandidates(Long conversationId, Long userId) {
        return aiRoundMemoryMapper.selectReadyRollupCandidates(conversationId, userId);
    }

    @Override
    public List<AiRoundMemory> listByIds(Collection<Long> ids) {
        return aiRoundMemoryMapper.selectListByIds(ids);
    }

    @Override
    public void create(AiRoundMemory roundMemory) {
        aiRoundMemoryMapper.insert(roundMemory);
    }

    @Override
    public void update(AiRoundMemory roundMemory) {
        aiRoundMemoryMapper.updateById(roundMemory);
    }

    @Override
    public void updateBatch(Collection<AiRoundMemory> roundMemories) {
        aiRoundMemoryMapper.updateBatch(roundMemories);
    }

    @Override
    public void deleteByConversationIdAndUserId(Long conversationId, Long userId) {
        aiRoundMemoryMapper.delete(new LambdaQueryWrapperX<AiRoundMemory>()
                .eq(AiRoundMemory::getConversationId, conversationId)
                .eq(AiRoundMemory::getUserId, userId));
    }
}
