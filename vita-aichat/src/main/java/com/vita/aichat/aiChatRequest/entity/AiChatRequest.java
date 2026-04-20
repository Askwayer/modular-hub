package com.vita.aichat.aiChatRequest.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI 请求生命周期表实体。
 */
@Data
@TableName("ai_chat_request")
@EqualsAndHashCode(callSuper = true)
public class AiChatRequest extends BaseEntity {

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("user_id")
    private Long userId;

    @TableField("request_id")
    private String requestId;

    @TableField("provider_code")
    private String providerCode;

    @TableField("provider_request_id")
    private String providerRequestId;

    @TableField("request_scene")
    private String requestScene;

    @TableField("request_mode")
    private String requestMode;

    @TableField("status")
    private String status;

    @TableField("user_message_id")
    private Long userMessageId;

    @TableField("assistant_message_id")
    private Long assistantMessageId;

    @TableField("model_code")
    private String modelCode;

    @TableField("finish_reason")
    private String finishReason;

    @TableField("token_input")
    private Integer tokenInput;

    @TableField("token_output")
    private Integer tokenOutput;

    @TableField("token_total")
    private Integer tokenTotal;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("first_token_at")
    private LocalDateTime firstTokenAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("ext_json")
    private String extJson;
}
