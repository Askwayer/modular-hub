package com.vita.system.associate.sysUserRole.mapper;

import com.vita.core.mapper.BaseMapperX;
import com.vita.system.associate.sysUserRole.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysUserRole.mapper
 * @Author znk
 * @CreateTime 2026-03-31 16:39:20
 * @Description: 用户角色关联表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapperX<SysUserRole> {

    /**
     * 查询用户角色ID集合。
     *
     * @param userId 用户ID
     * @return 角色ID集合
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
