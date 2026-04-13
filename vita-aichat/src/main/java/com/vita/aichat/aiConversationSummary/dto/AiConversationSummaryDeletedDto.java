package com.vita.aichat.aiConversationSummary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 删除 DTO
 * @version 1.0
 */
@Data
public class AiConversationSummaryDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
