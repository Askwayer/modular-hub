package com.vita.system.sysRole.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
