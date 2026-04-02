package com.vita.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.vita.auth.service.IAuthCommonService;
import com.vita.constant.AuthConstants;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.enums.CommonStatusEnum;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service.impl
 * @Author: znk
 * @CreateTime: 2026-04-02  15:43:20
 * @Description: 认证公共服务实现类，提供认证相关的公共方法的具体实现
 * @Version: 1.0
 */
@Service
public class AuthCommonServiceImpl implements IAuthCommonService {

    private final SysUserMapper sysUserMapper;
    private final ISysUserRoleService sysUserRoleService;
    private final ISysRolePermissionService sysRolePermissionService;
    private final ISysPermissionService sysPermissionService;
    private final ISysRoleMenuService sysRoleMenuService;
    private final ISysMenuService sysMenuService;

    public AuthCommonServiceImpl(SysUserMapper sysUserMapper,
                                 ISysUserRoleService sysUserRoleService,
                                 ISysRolePermissionService sysRolePermissionService,
                                 ISysPermissionService sysPermissionService,
                                 ISysRoleMenuService sysRoleMenuService,
                                 ISysMenuService sysMenuService) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleService = sysUserRoleService;
        this.sysRolePermissionService = sysRolePermissionService;
        this.sysPermissionService = sysPermissionService;
        this.sysRoleMenuService = sysRoleMenuService;
        this.sysMenuService = sysMenuService;
    }

    /**
     * 根据用户ID获取其拥有的角色编码列表。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        SysUser user = getActiveUser(userId);
        if (user == null) {
            return CollUtil.newArrayList();
        }
        if (CommonStatusEnum.isEnabled(user.getIsSuperAdmin())) {
            return CollUtil.newArrayList(AuthConstants.SUPER_ADMIN_ROLE_CODE);
        }
        return sysUserRoleService.getRoleCodesByUserId(userId);
    }

    /**
     * 根据用户ID获取拥有的权限编码列表。
     *
     * @param userId 用户 ID
     * @return 权限code集合
     */
    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        SysUser user = getActiveUser(userId);
        if (user == null) {
            return CollUtil.newArrayList();
        }
        if (CommonStatusEnum.isEnabled(user.getIsSuperAdmin())) {
            return CollUtil.newArrayList(AuthConstants.ALL_PERMISSION_CODE);
        }
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        List<Long> permissionIds = sysRolePermissionService.getPermissionIdsByRoleIds(roleIds);
        if (CollUtil.isEmpty(permissionIds)) {
            return CollUtil.newArrayList();
        }
        return sysPermissionService.getPermissionCodesByIds(permissionIds, CommonStatusEnum.ENABLED.getCode());
    }

    /**
     * 根据用户ID获取其拥有的菜单列表。
     *
     * @param userId 用户 ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        SysUser user = getActiveUser(userId);
        if (user == null) {
            return CollUtil.newArrayList();
        }
        if (CommonStatusEnum.isEnabled(user.getIsSuperAdmin())) {
            return sysMenuService.getAllEnabledMenus();
        }
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        List<Long> menuIds = sysRoleMenuService.getMenuIdsByRoleIds(roleIds);
        if (CollUtil.isEmpty(menuIds)) {
            return CollUtil.newArrayList();
        }
        return sysMenuService.getListByIds(menuIds, CommonStatusEnum.ENABLED.getCode());
    }

    /**
     * 判断用户是否是超级管理员。
     *
     * @param userId 用户 ID
     * @return 是-true/否-false
     */
    @Override
    public boolean isSuperAdmin(Long userId) {
        SysUser user = getActiveUser(userId);
        return user != null && CommonStatusEnum.isEnabled(user.getIsSuperAdmin());
    }

    /**
     * 根据 userId 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    private SysUser getActiveUser(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapperX<SysUser>()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getStatus, CommonStatusEnum.ENABLED.getCode()));
    }
}
