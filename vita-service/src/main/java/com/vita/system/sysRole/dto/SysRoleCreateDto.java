package com.vita.system.sysRole.dto;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 创建 DTO
 * @version 1.0
 */
@Data
public class SysRoleCreateDto {
    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 显示顺序
     */
    private Integer roleSort;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 数据范围 ALL/DEPT_AND_CHILD/DEPT_SELF/SELF/CUSTOM
     */
    private String dataScopeType;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 备注
     */
    private String remark;

}
