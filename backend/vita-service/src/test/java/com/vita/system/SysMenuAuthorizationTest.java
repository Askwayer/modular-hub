package com.vita.system;

import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysMenu.service.impl.SysMenuServiceImpl;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SysMenuAuthorizationTest {
    @Test
    void menuQueriesShouldExcludeDisabledRolesAndStopWhenAllRolesAreDisabled() {
        ISysUserService users = mock(ISysUserService.class);
        ISysUserRoleService userRoles = mock(ISysUserRoleService.class);
        ISysRoleService roles = mock(ISysRoleService.class);
        ISysRoleMenuService roleMenus = mock(ISysRoleMenuService.class);
        SysMenuServiceImpl menus = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(menus, "sysUserService", users);
        ReflectionTestUtils.setField(menus, "sysUserRoleService", userRoles);
        ReflectionTestUtils.setField(menus, "sysRoleService", roles);
        ReflectionTestUtils.setField(menus, "sysRoleMenuService", roleMenus);
        when(users.getActiveById(7L)).thenReturn(new SysUser());
        when(userRoles.getRoleIdsByUserId(7L)).thenReturn(List.of(10L, 20L));
        when(roles.getEnabledIdsByIds(List.of(10L, 20L))).thenReturn(List.of(10L), List.of());
        when(roleMenus.getMenuIdsByRoleIds(List.of(10L))).thenReturn(List.of());

        assertThat(menus.getMenusByUserId(7L)).isEmpty();
        assertThat(menus.getMenusByUserId(7L)).isEmpty();
        verify(roleMenus, times(1)).getMenuIdsByRoleIds(List.of(10L));
        verifyNoMoreInteractions(roleMenus);
    }
}
