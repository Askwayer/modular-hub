package com.vita.aichat.aiConversation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 删除 DTO
 * @version 1.0
 */
@Data
public class AiConversationDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
