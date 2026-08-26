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
 * @Description: App用户表 实体类
 * @version 1.0
 */
@Data
@TableName("app_user")
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseEntity {

    /**
     * 用户名称
     */
    @TableField("user_name")
    private String userName;

    /**
     * BCrypt密码摘要
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户昵称
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * 头像地址
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 最后登录时间
     */
    @TableField("login_time")
    private LocalDateTime loginTime;

    /**
     * 最后登录地址
     */
    @TableField("login_address")
    private String loginAddress;

    /**
     * 密码最后更新时间
     */
    @TableField("pwd_update_date")
    private LocalDateTime pwdUpdateDate;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
