package com.vita.system.sysUser.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.vo
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 列表 VO
 * @version 1.0
 */
@Data
public class SysUserListVo {
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
    private String userName;

    /**
     * 密码
     */
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
     * 状态(0-正常,1-禁用)
     */
    private String status;

    /**
     * 是否为超级管理员(0-否,1-是)
     */
    private String isSuper;

    /**
     * 上次登录时间
     */
    private java.time.LocalDateTime loginTime;

    /**
     * 登录地址
     */
    private String loginAddress;

    /**
     * 密码最后更新时间
     */
    private java.time.LocalDateTime pwdUpdateDate;

    /**
     * 备注
     */
    private String remark;

}
