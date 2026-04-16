package com.vita.aichat.aiMessageStreamBatch.service;

import com.vita.aichat.aiMessageStreamBatch.entity.AiMessageStreamBatch;

import java.util.Collection;
import java.util.List;

/**
 * 流式批次快照表读写服务。
 */
public interface IAiMessageStreamBatchService {

    void create(AiMessageStreamBatch batch);

    void createBatch(List<AiMessageStreamBatch> batches);

    List<AiMessageStreamBatch> listByAssistantMessageId(Long assistantMessageId);

    void deleteByAssistantMessageIds(Collection<Long> assistantMessageIds);
}
