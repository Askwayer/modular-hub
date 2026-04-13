package com.vita.aichat.aiConversationSummary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiConversationSummaryUpdateDto extends AiConversationSummaryCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
