package com.vita.system.sysUser.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.dto
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 创建 DTO
 */
@Data
public class SysUserCreateDto {
    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 用户名称
     */
    @NotBlank(message = "【用户名称】不能为空")
    private String userName;

    /**
     * 密码
     */
    @NotBlank(message = "【密码】不能为空")
    private String password;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 性别(0-女,1-男,2-其他)
     */
    private String gender;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 是否超级管理员 1是 0否
     */
    private Byte isSuperAdmin;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 上次登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /**
     * 登录地址
     */
    private String loginAddress;

    /**
     * 密码最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pwdUpdateDate;

    /**
     * 备注
     */
    private String remark;

}
