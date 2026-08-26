package com.vita.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.entity
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App OAuth账号绑定表 实体类
 * @version 1.0
 */
@Data
@TableName("app_oauth_account")
@EqualsAndHashCode(callSuper = true)
public class AppOauthAccount extends BaseEntity {

    /**
     * App用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * OAuth平台编码
     */
    @TableField("provider_code")
    private String providerCode;

    /**
     * OAuth平台用户ID
     */
    @TableField("provider_user_id")
    private String providerUserId;

    /**
     * 平台登录名快照
     */
    @TableField("provider_login")
    private String providerLogin;

    /**
     * 平台昵称快照
     */
    @TableField("provider_nick_name")
    private String providerNickName;

    /**
     * 平台头像快照
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 平台公开主页
     */
    @TableField("profile_url")
    private String profileUrl;

    /**
     * 首次绑定时间
     */
    @TableField("bind_time")
    private LocalDateTime bindTime;

    /**
     * 最近OAuth认证时间
     */
    @TableField("last_auth_time")
    private LocalDateTime lastAuthTime;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
