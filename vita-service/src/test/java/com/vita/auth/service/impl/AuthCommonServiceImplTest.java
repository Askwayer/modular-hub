package com.vita.auth.service.impl;

import com.vita.constant.AuthConstants;
import com.vita.constant.Constants;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCommonServiceImplTest {

    @Mock
    private ISysUserService sysUserService;
    @Mock
    private ISysUserRoleService sysUserRoleService;
    @Mock
    private ISysRolePermissionService sysRolePermissionService;
    @Mock
    private ISysPermissionService sysPermissionService;
    @Mock
    private ISysRoleService sysRoleService;

    @Test
    void getRoleCodesByUserIdShouldReturnSuperAdminRoleWhenUserIsSuperAdmin() {
        AuthCommonServiceImpl authCommonService = buildAuthCommonService();
        when(sysUserService.getActiveById(1L)).thenReturn(buildUser(1L, Constants.ENABLED));

        List<String> roleCodes = authCommonService.getRoleCodesByUserId(1L);

        assertEquals(List.of(AuthConstants.SUPER_ADMIN_ROLE_CODE), roleCodes);
        verify(sysUserRoleService, never()).getRoleIdsByUserId(1L);
        verify(sysRoleService, never()).getRoleCodesByIds(List.of(), Constants.ENABLED);
    }

    @Test
    void getRoleCodesByUserIdShouldAssembleCodesByRoleIds() {
        AuthCommonServiceImpl authCommonService = buildAuthCommonService();
        when(sysUserService.getActiveById(2L)).thenReturn(buildUser(2L, Constants.DISABLED));
        when(sysUserRoleService.getRoleIdsByUserId(2L)).thenReturn(List.of(2L, 1L));
        when(sysRoleService.getRoleCodesByIds(List.of(2L, 1L), Constants.ENABLED))
                .thenReturn(List.of("SYSTEM_ADMIN", "AUDITOR"));

        List<String> roleCodes = authCommonService.getRoleCodesByUserId(2L);

        assertEquals(List.of("SYSTEM_ADMIN", "AUDITOR"), roleCodes);
        verify(sysUserRoleService).getRoleIdsByUserId(2L);
        verify(sysRoleService).getRoleCodesByIds(List.of(2L, 1L), Constants.ENABLED);
    }

    private AuthCommonServiceImpl buildAuthCommonService() {
        return new AuthCommonServiceImpl(
                sysUserService,
                sysUserRoleService,
                sysRolePermissionService,
                sysPermissionService,
                sysRoleService
        );
    }

    private SysUser buildUser(Long userId, Byte isSuperAdmin) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setIsSuperAdmin(isSuperAdmin);
        user.setStatus(Constants.ENABLED);
        return user;
    }
}
