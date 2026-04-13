package com.vita.aichat.aiConversation.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 创建 DTO
 * @version 1.0
 */
@Data
public class AiConversationCreateDto {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 标题来源 AUTO/MANUAL
     */
    private String titleSource;

    /**
     * 状态 ACTIVE/ARCHIVED/DELETED
     */
    private String status;

    /**
     * 是否置顶 0否 1是
     */
    private Byte pinned;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageAt;

    /**
     * 消息总数
     */
    private Integer messageCount;

    /**
     * 轮次数 1轮=问+答
     */
    private Integer roundCount;

    /**
     * 当前模型ID
     */
    private Long currentModelId;

    /**
     * 当前模型编码
     */
    private String currentModelCode;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 温度参数
     */
    private java.math.BigDecimal temperature;

    /**
     * 单次最大输出token
     */
    private Integer maxTokens;

    /**
     * 最大上下文消息数
     */
    private Integer maxContextMessages;

    /**
     * 当前摘要版本
     */
    private Integer summaryVersion;

    /**
     * 摘要状态 NONE/READY/STALE/GENERATING
     */
    private String summaryStatus;

    /**
     * 扩展字段
     */
    private String extJson;

}
