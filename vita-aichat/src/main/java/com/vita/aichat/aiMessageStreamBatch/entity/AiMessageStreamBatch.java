package com.vita.aichat.aiMessageStreamBatch.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 流式批次快照表实体。
 */
@Data
@TableName("ai_message_stream_batch")
@EqualsAndHashCode(callSuper = true)
public class AiMessageStreamBatch extends BaseEntity {

    @TableField("assistant_message_id")
    private Long assistantMessageId;

    @TableField("request_id")
    private String requestId;

    @TableField("batch_no")
    private Integer batchNo;

    @TableField("content_delta")
    private String contentDelta;

    @TableField("reasoning_delta")
    private String reasoningDelta;

    @TableField("flush_reason")
    private String flushReason;

    @TableField("delta_chars")
    private Integer deltaChars;

    @TableField("provider_sequence")
    private Integer providerSequence;
}
