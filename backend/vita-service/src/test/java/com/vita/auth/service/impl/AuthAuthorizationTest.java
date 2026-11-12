package com.vita.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.vita.auth.StpInterfaceImpl;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 鉴权使用真实聚合链路，模拟提交后的状态变化，验证旧授权不会继续生效。
 */
class AuthAuthorizationTest {
    private final ISysUserService users = mock(ISysUserService.class);
    private final ISysUserRoleService userRoles = mock(ISysUserRoleService.class);
    private final ISysRolePermissionService rolePermissions = mock(ISysRolePermissionService.class);
    private final ISysPermissionService permissions = mock(ISysPermissionService.class);
    private final ISysRoleService roles = mock(ISysRoleService.class);
    private final AuthCommonServiceImpl auth = new AuthCommonServiceImpl(users, userRoles, rolePermissions, permissions, roles);
    private final StpInterfaceImpl stp = new StpInterfaceImpl(auth);
    private final Byte enabled = CommonStatusEnum.ENABLED.getCode();

    @BeforeEach
    void setUp() {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(enabled);
        user.setIsSuperAdmin((byte) 0);
        when(users.getActiveById(7L)).thenReturn(user);
        when(userRoles.getRoleIdsByUserId(7L)).thenReturn(List.of(10L, 20L));
        when(roles.getEnabledIdsByIds(List.of(10L, 20L))).thenReturn(List.of(10L));
        when(rolePermissions.getPermissionIdsByRoleIds(List.of(10L))).thenReturn(List.of(100L));
        when(permissions.getPermissionCodesByIds(List.of(100L), enabled)).thenReturn(List.of("purchase:read"));
    }

    @Test
    void disabledRoleMustNeverContributePermissions() {
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).containsExactly("purchase:read");
        verify(rolePermissions).getPermissionIdsByRoleIds(List.of(10L));
        verify(rolePermissions, never()).getPermissionIdsByRoleIds(List.of(10L, 20L));
        when(roles.getEnabledIdsByIds(List.of(10L, 20L))).thenReturn(List.of());
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isEmpty();
    }

    @Test
    void revokedUserRoleShouldTakeEffectOnNextCheck() {
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isNotEmpty();
        when(userRoles.getRoleIdsByUserId(7L)).thenReturn(List.of());
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isEmpty();
        assertThat(stp.getRoleList(7L, StpUtil.TYPE)).isEmpty();
    }

    @Test
    void revokedRolePermissionAndDisabledPermissionShouldTakeEffect() {
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isNotEmpty();
        when(permissions.getPermissionCodesByIds(List.of(100L), enabled)).thenReturn(List.of());
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isEmpty();
        when(rolePermissions.getPermissionIdsByRoleIds(List.of(10L))).thenReturn(List.of());
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isEmpty();
    }

    @Test
    void disabledOrDeletedAccountMustLoseSuperAdminPrivileges() {
        SysUser admin = new SysUser();
        admin.setIsSuperAdmin(enabled);
        when(users.getActiveById(7L)).thenReturn(admin);
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).containsExactly(AuthConstants.ALL_PERMISSION_CODE);
        when(users.getActiveById(7L)).thenReturn(null);
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isEmpty();
        assertThat(stp.getRoleList(7L, StpUtil.TYPE)).isEmpty();
        assertThat(auth.isSuperAdmin(7L)).isFalse();
    }

    @Test
    void queryingAnotherAccountMustNotReusePreviousAccountPermissions() {
        assertThat(stp.getPermissionList(7L, StpUtil.TYPE)).isNotEmpty();
        assertThat(stp.getPermissionList(8L, StpUtil.TYPE)).isEmpty();
        verify(users).getActiveById(8L);
    }

    @Test
    void invalidIdsAndUnknownLoginDomainMustNotQueryAccounts() {
        clearInvocations(users);
        for (Object id : new Object[]{null, "bad", 0L, -1L}) {
            assertThat(stp.getPermissionList(id, StpUtil.TYPE)).isEmpty();
            assertThat(stp.getRoleList(id, StpUtil.TYPE)).isEmpty();
        }
        assertThat(stp.getPermissionList(7L, "unknown")).isEmpty();
        verifyNoInteractions(users);
    }
}
