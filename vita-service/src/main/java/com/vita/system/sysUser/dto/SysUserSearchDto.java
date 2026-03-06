package com.vita.system.sysUser.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.dto
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
