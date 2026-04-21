package com.vita.aichat.aiRoundMemory.service;

import com.vita.aichat.aiRoundMemory.entity.AiRoundMemory;

import java.util.Collection;
import java.util.List;

/**
 * 会话轮次记忆读写服务。
 */
public interface IAiRoundMemoryService {

    AiRoundMemory getLatestRoundMemory(Long conversationId, Long userId);

    List<AiRoundMemory> listReadyCarryForwardMemories(Long conversationId, Long userId);

    List<AiRoundMemory> listReadyRollupCandidates(Long conversationId, Long userId);

    List<AiRoundMemory> listByIds(Collection<Long> ids);

    void create(AiRoundMemory roundMemory);

    void update(AiRoundMemory roundMemory);

    void updateBatch(Collection<AiRoundMemory> roundMemories);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
