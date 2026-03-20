package com.vita.system.sysIpBlock.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.dto
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysIpBlockSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
