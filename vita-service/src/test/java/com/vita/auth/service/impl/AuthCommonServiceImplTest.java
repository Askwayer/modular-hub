package com.vita.auth.service.impl;

import com.vita.constant.AuthConstants;
import com.vita.constant.Constants;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCommonServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private ISysUserRoleService sysUserRoleService;
    @Mock
    private ISysRolePermissionService sysRolePermissionService;
    @Mock
    private ISysPermissionService sysPermissionService;
    @Mock
    private ISysRoleMenuService sysRoleMenuService;
    @Mock
    private ISysMenuService sysMenuService;

    @Test
    void shouldReturnVirtualSuperAdminDataWhenUserIsSuperAdmin() {
        AuthCommonServiceImpl authCommonService = new AuthCommonServiceImpl(
                sysUserMapper,
                sysUserRoleService,
                sysRolePermissionService,
                sysPermissionService,
                sysRoleMenuService,
                sysMenuService
        );
        SysUser user = buildUser(Constants.ENABLED);
        SysMenu menu = new SysMenu();
        menu.setId(1000L);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysMenuService.getAllEnabledMenus()).thenReturn(List.of(menu));

        assertTrue(authCommonService.isSuperAdmin(1L));
        assertEquals(List.of(AuthConstants.SUPER_ADMIN_ROLE_CODE), authCommonService.getRoleCodesByUserId(1L));
        assertEquals(List.of(AuthConstants.ALL_PERMISSION_CODE), authCommonService.getPermissionCodesByUserId(1L));
        assertEquals(List.of(menu), authCommonService.getMenusByUserId(1L));
        verify(sysMenuService).getAllEnabledMenus();
    }

    @Test
    void shouldQueryAssociationTablesWhenUserIsNormalUser() {
        AuthCommonServiceImpl authCommonService = new AuthCommonServiceImpl(
                sysUserMapper,
                sysUserRoleService,
                sysRolePermissionService,
                sysPermissionService,
                sysRoleMenuService,
                sysMenuService
        );
        SysUser user = buildUser(Constants.DISABLED);
        SysMenu menu = new SysMenu();
        menu.setId(2000L);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysUserRoleService.getRoleCodesByUserId(2L)).thenReturn(List.of("SYSTEM_ADMIN"));
        when(sysUserRoleService.getRoleIdsByUserId(2L)).thenReturn(List.of(10L, 20L));
        when(sysRolePermissionService.getPermissionIdsByRoleIds(List.of(10L, 20L))).thenReturn(List.of(1000L, 2000L));
        when(sysPermissionService.getPermissionCodesByIds(List.of(1000L, 2000L), Constants.ENABLED))
                .thenReturn(List.of("system:user:view"));
        when(sysRoleMenuService.getMenuIdsByRoleIds(List.of(10L, 20L))).thenReturn(List.of(100L, 200L));
        when(sysMenuService.getListByIds(List.of(100L, 200L), Constants.ENABLED)).thenReturn(List.of(menu));

        assertEquals(List.of("SYSTEM_ADMIN"), authCommonService.getRoleCodesByUserId(2L));
        assertEquals(List.of("system:user:view"), authCommonService.getPermissionCodesByUserId(2L));
        assertEquals(List.of(menu), authCommonService.getMenusByUserId(2L));
    }

    private SysUser buildUser(Byte isSuperAdmin) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(Constants.ENABLED);
        user.setIsSuperAdmin(isSuperAdmin);
        user.setIsDeleted(Boolean.FALSE);
        return user;
    }
}
