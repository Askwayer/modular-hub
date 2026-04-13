package com.vita.aichat.aiMessage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiMessage.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiMessageUpdateDto extends AiMessageCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
