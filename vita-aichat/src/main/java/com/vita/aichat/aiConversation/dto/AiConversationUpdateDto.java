package com.vita.aichat.aiConversation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiConversationUpdateDto extends AiConversationCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
