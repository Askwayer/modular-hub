package com.vita.app.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.dto
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App OAuth账号绑定表 创建 DTO
 * @version 1.0
 */
@Data
public class AppOauthAccountCreateDto {
    /**
     * App用户ID
     */
    private Long userId;

    /**
     * OAuth平台编码
     */
    private String providerCode;

    /**
     * OAuth平台用户ID
     */
    private String providerUserId;

    /**
     * 平台登录名快照
     */
    private String providerLogin;

    /**
     * 平台昵称快照
     */
    private String providerNickName;

    /**
     * 平台头像快照
     */
    private String avatarUrl;

    /**
     * 平台公开主页
     */
    private String profileUrl;

    /**
     * 首次绑定时间
     */
    private LocalDateTime bindTime;

    /**
     * 最近OAuth认证时间
     */
    private LocalDateTime lastAuthTime;

    /**
     * 备注
     */
    private String remark;

}
