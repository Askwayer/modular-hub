package com.vita.app.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.dto
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App用户表 创建 DTO
 * @version 1.0
 */
@Data
public class AppUserCreateDto {
    /**
     * 用户名称
     */
    private String userName;

    /**
     * BCrypt密码摘要
     */
    private String passwordHash;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 最后登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 最后登录地址
     */
    private String loginAddress;

    /**
     * 密码最后更新时间
     */
    private LocalDateTime pwdUpdateDate;

    /**
     * 备注
     */
    private String remark;

}
