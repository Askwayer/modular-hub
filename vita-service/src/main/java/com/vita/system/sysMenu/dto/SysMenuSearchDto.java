package com.vita.system.sysMenu.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenuSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
