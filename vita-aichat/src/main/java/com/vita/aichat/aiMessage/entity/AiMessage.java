package com.vita.aichat.aiMessage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiMessage.entity
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 实体类
 * @version 1.0
 */
@Data
@TableName("ai_message")
@EqualsAndHashCode(callSuper = true)
public class AiMessage extends BaseEntity {

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色 USER/ASSISTANT/SYSTEM/SUMMARY
     */
    @TableField("role_type")
    private String roleType;

    /**
     * 消息类型 TEXT/IMAGE/FILE/EVENT
     */
    @TableField("message_type")
    private String messageType;

    /**
     * 回复消息ID，ASSISTANT通常指向对应USER消息
     */
    @TableField("reply_to_id")
    private Long replyToId;

    /**
     * 请求ID，前后端链路追踪
     */
    @TableField("request_id")
    private String requestId;

    /**
     * 模型ID
     */
    @TableField("model_id")
    private Long modelId;

    /**
     * 模型编码
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 最终展示内容
     */
    @TableField("content")
    private String content;

    /**
     * 推理内容或思考内容
     */
    @TableField("reasoning_content")
    private String reasoningContent;

    /**
     * 内容状态 GENERATING/DONE/ERROR/CANCELLED
     */
    @TableField("content_status")
    private String contentStatus;

    /**
     * 是否参与上下文 0否 1是
     */
    @TableField("use_context")
    private Byte useContext;

    /**
     * 输入token
     */
    @TableField("token_input")
    private Integer tokenInput;

    /**
     * 输出token
     */
    @TableField("token_output")
    private Integer tokenOutput;

    /**
     * 总token
     */
    @TableField("token_total")
    private Integer tokenTotal;

    /**
     * 错误码
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 附件信息 JSON
     */
    @TableField("attachments_json")
    private String attachmentsJson;

    /**
     * 扩展信息 JSON
     */
    @TableField("extra_json")
    private String extraJson;
}
