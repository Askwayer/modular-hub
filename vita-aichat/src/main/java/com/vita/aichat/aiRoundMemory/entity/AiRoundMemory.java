package com.vita.aichat.aiRoundMemory.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 会话轮次记忆。
 */
@Data
@TableName("ai_round_memory")
@EqualsAndHashCode(callSuper = true)
public class AiRoundMemory extends BaseEntity {

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("user_id")
    private Long userId;

    @TableField("round_no")
    private Integer roundNo;

    @TableField("source_request_id")
    private String sourceRequestId;

    @TableField("source_user_message_id")
    private Long sourceUserMessageId;

    @TableField("source_assistant_message_id")
    private Long sourceAssistantMessageId;

    @TableField("provider_code")
    private String providerCode;

    @TableField("model_code")
    private String modelCode;

    @TableField("memory_text")
    private String memoryText;

    @TableField("memory_json")
    private String memoryJson;

    @TableField("prompt_token_estimate")
    private Integer promptTokenEstimate;

    @TableField("salience_score")
    private Integer salienceScore;

    @TableField("carry_forward")
    private Byte carryForward;

    @TableField("status")
    private String status;
}
