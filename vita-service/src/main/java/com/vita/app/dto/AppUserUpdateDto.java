package com.vita.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.dto
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App用户表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppUserUpdateDto extends AppUserCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
