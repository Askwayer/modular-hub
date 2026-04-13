package com.vita.aichat.aiMessage.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiMessage.vo
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 列表 VO
 * @version 1.0
 */
@Data
public class AiMessageListVo {
    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 角色 USER/ASSISTANT/SYSTEM/SUMMARY
     */
    private String roleType;

    /**
     * 消息类型 TEXT/IMAGE/FILE/EVENT
     */
    private String messageType;

    /**
     * 回复消息ID，ASSISTANT通常指向对应USER消息
     */
    private Long replyToId;

    /**
     * 请求ID，前后端链路追踪
     */
    private String requestId;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 模型编码
     */
    private String modelCode;

    /**
     * 最终展示内容
     */
    private String content;

    /**
     * 推理内容或思考内容
     */
    private String reasoningContent;

    /**
     * 内容状态 GENERATING/DONE/ERROR/CANCELLED
     */
    private String contentStatus;

    /**
     * 是否参与上下文 0否 1是
     */
    private Byte useContext;

    /**
     * 输入token
     */
    private Integer tokenInput;

    /**
     * 输出token
     */
    private Integer tokenOutput;

    /**
     * 总token
     */
    private Integer tokenTotal;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 附件信息 JSON
     */
    private String attachmentsJson;

    /**
     * 扩展信息 JSON
     */
    private String extraJson;

}
