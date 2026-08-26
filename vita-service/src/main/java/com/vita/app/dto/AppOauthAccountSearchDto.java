package com.vita.app.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.dto
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App OAuth账号绑定表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppOauthAccountSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
