package com.vita.aichat.aiConversationSummary.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.vo
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 列表 VO
 * @version 1.0
 */
@Data
public class AiConversationSummaryListVo {
    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 摘要版本号
     */
    private Integer versionNo;

    /**
     * 压缩起始消息ID
     */
    private Long sourceMessageStartId;

    /**
     * 压缩结束消息ID
     */
    private Long sourceMessageEndId;

    /**
     * 被压缩消息数
     */
    private Integer sourceMessageCount;

    /**
     * 摘要文本
     */
    private String summaryText;

    /**
     * 结构化摘要 JSON
     */
    private String summaryJson;

    /**
     * 预估节省token
     */
    private Integer tokenSavedEstimate;

    /**
     * 状态 READY/STALE
     */
    private String status;

}
