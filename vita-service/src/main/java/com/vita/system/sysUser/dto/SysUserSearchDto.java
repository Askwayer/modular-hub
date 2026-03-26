package com.vita.system.sysUser.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.dto
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 搜索 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserSearchDto extends PageRequest {

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 昵称
     */
    private String nickName;


    /**
     * 手机号
     */
    private String mobile;


    /**
     * 状态(0-正常,1-禁用)
     */
    private String status;


}
