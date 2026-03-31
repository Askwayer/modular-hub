package com.vita.system.sysUser.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.entity
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 实体类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * 部门ID
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 昵称
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * 用户名称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String mobile;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 头像
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 性别(0-女,1-男,2-其他)
     */
    @TableField("gender")
    private String gender;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 是否为超级管理员(0-否,1-是)
     */
    @TableField("is_super")
    private String isSuper;

    /**
     * 上次登录时间
     */
    @TableField("login_time")
    private LocalDateTime loginTime;

    /**
     * 登录地址
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
