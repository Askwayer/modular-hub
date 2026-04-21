package com.vita.aichat.aiConversation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.entity
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 实体类
 * @version 1.0
 */
@Data
@TableName("ai_conversation")
@EqualsAndHashCode(callSuper = true)
public class AiConversation extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话标题
     */
    @TableField("title")
    private String title;

    /**
     * 标题来源 AUTO/MANUAL
     */
    @TableField("title_source")
    private String titleSource;

    /**
     * 状态 ACTIVE/ARCHIVED/DELETED
     */
    @TableField("status")
    private String status;

    /**
     * 是否置顶 0否 1是
     */
    @TableField("pinned")
    private Byte pinned;

    /**
     * 最后消息时间
     */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 消息总数
     */
    @TableField("message_count")
    private Integer messageCount;

    /**
     * 轮次数 1轮=问+答
     */
    @TableField("round_count")
    private Integer roundCount;

    /**
     * 当前模型ID
     */
    @TableField("current_model_id")
    private Long currentModelId;

    /**
     * 当前模型提供者编码
     */
    @TableField("current_provider_code")
    private String currentProviderCode;

    /**
     * 当前模型编码
     */
    @TableField("current_model_code")
    private String currentModelCode;

    /**
     * 系统提示词
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 温度参数
     */
    @TableField("temperature")
    private BigDecimal temperature;

    /**
     * 单次最大输出token
     */
    @TableField("max_tokens")
    private Integer maxTokens;

    /**
     * 最大上下文消息数
     */
    @TableField("max_context_messages")
    private Integer maxContextMessages;

    /**
     * 当前摘要版本
     */
    @TableField("summary_version")
    private Integer summaryVersion;

    /**
     * 摘要状态 NONE/READY/STALE/GENERATING
     */
    @TableField("summary_status")
    private String summaryStatus;

    /**
     * 当前生成中的请求ID
     */
    @TableField("active_request_id")
    private String activeRequestId;

    /**
     * 最近一次失败错误码
     */
    @TableField("last_error_code")
    private String lastErrorCode;

    /**
     * 最近一次失败错误信息
     */
    @TableField("last_error_message")
    private String lastErrorMessage;

    /**
     * 扩展字段
     */
    @TableField("ext_json")
    private String extJson;
}
