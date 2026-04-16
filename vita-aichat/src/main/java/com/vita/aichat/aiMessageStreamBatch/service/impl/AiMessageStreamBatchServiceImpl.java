package com.vita.aichat.aiMessageStreamBatch.service.impl;

import com.vita.aichat.aiMessageStreamBatch.entity.AiMessageStreamBatch;
import com.vita.aichat.aiMessageStreamBatch.mapper.AiMessageStreamBatchMapper;
import com.vita.aichat.aiMessageStreamBatch.service.IAiMessageStreamBatchService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 流式批次快照表读写服务实现。
 */
@Service
public class AiMessageStreamBatchServiceImpl implements IAiMessageStreamBatchService {

    private final AiMessageStreamBatchMapper aiMessageStreamBatchMapper;

    public AiMessageStreamBatchServiceImpl(AiMessageStreamBatchMapper aiMessageStreamBatchMapper) {
        this.aiMessageStreamBatchMapper = aiMessageStreamBatchMapper;
    }

    @Override
    public void create(AiMessageStreamBatch batch) {
        aiMessageStreamBatchMapper.insert(batch);
    }

    @Override
    public void createBatch(List<AiMessageStreamBatch> batches) {
        if (batches == null || batches.isEmpty()) {
            return;
        }
        aiMessageStreamBatchMapper.insertBatch(batches);
    }

    @Override
    public List<AiMessageStreamBatch> listByAssistantMessageId(Long assistantMessageId) {
        return aiMessageStreamBatchMapper.selectListByAssistantMessageId(assistantMessageId);
    }

    @Override
    public void deleteByAssistantMessageIds(Collection<Long> assistantMessageIds) {
        aiMessageStreamBatchMapper.deleteByAssistantMessageIds(assistantMessageIds);
    }
}
