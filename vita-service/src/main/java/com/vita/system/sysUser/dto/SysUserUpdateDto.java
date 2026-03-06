package com.vita.system.sysUser.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.dto
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserUpdateDto extends SysUserCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
