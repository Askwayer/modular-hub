package com.vita.aichat.aiConversationSummary.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.entity
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 实体类
 * @version 1.0
 */
@Data
@TableName("ai_conversation_summary")
@EqualsAndHashCode(callSuper = true)
public class AiConversationSummary extends BaseEntity {

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 摘要版本号
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 生成摘要时使用的模型提供者
     */
    @TableField("provider_code")
    private String providerCode;

    /**
     * 生成摘要时使用的模型编码
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 压缩起始消息ID
     */
    @TableField("source_message_start_id")
    private Long sourceMessageStartId;

    /**
     * 压缩结束消息ID
     */
    @TableField("source_message_end_id")
    private Long sourceMessageEndId;

    /**
     * 被压缩消息数
     */
    @TableField("source_message_count")
    private Integer sourceMessageCount;

    /**
     * 压缩起始轮次号
     */
    @TableField("source_round_start_no")
    private Integer sourceRoundStartNo;

    /**
     * 压缩结束轮次号
     */
    @TableField("source_round_end_no")
    private Integer sourceRoundEndNo;

    /**
     * 压缩起始轮次记忆ID
     */
    @TableField("source_round_memory_start_id")
    private Long sourceRoundMemoryStartId;

    /**
     * 压缩结束轮次记忆ID
     */
    @TableField("source_round_memory_end_id")
    private Long sourceRoundMemoryEndId;

    /**
     * 摘要文本
     */
    @TableField("summary_text")
    private String summaryText;

    /**
     * 结构化摘要 JSON
     */
    @TableField("summary_json")
    private String summaryJson;

    /**
     * 预估节省token
     */
    @TableField("token_saved_estimate")
    private Integer tokenSavedEstimate;

    /**
     * 注入摘要时的 token 估算
     */
    @TableField("prompt_token_estimate")
    private Integer promptTokenEstimate;

    /**
     * 状态 READY/STALE
     */
    @TableField("status")
    private String status;
}
