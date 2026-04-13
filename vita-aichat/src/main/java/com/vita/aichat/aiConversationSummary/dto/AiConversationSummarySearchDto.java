package com.vita.aichat.aiConversationSummary.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiConversationSummarySearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
