package com.vita.system.associate.sysUserRole.service.impl;

import com.vita.core.exception.ServiceException;
import com.vita.enums.CommonStatusEnum;
import com.vita.system.associate.sysUserRole.mapper.SysUserRoleMapper;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.service.ISysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserRoleServiceImplTest {

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private ISysRoleService sysRoleService;

    private SysUserRoleServiceImpl sysUserRoleService;

    @BeforeEach
    void setUp() {
        sysUserRoleService = new SysUserRoleServiceImpl();
        ReflectionTestUtils.setField(sysUserRoleService, "sysUserRoleMapper", sysUserRoleMapper);
        ReflectionTestUtils.setField(sysUserRoleService, "sysRoleService", sysRoleService);
    }

    @Test
    void getRoleIdsByUserIdShouldRejectNullUserId() {
        assertThrows(ServiceException.class, () -> sysUserRoleService.getRoleIdsByUserId(null));
    }

    @Test
    void getRoleIdsByUserIdShouldReturnDistinctRoleIds() {
        when(sysUserRoleMapper.selectRoleIdsByUserId(100L)).thenReturn(List.of(1L, 2L));

        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(100L);

        assertEquals(List.of(1L, 2L), roleIds);
    }

    @Test
    void getRoleCodesByUserIdShouldAssembleRoleCodesWithoutJoinSql() {
        when(sysUserRoleMapper.selectRoleIdsByUserId(100L)).thenReturn(List.of(2L, 1L));
        when(sysRoleService.getListByIds(List.of(2L, 1L), CommonStatusEnum.ENABLED.getCode())).thenReturn(List.of(
                buildRole(1L, "SYSTEM_ADMIN", 1),
                buildRole(2L, "AUDITOR", 2)
        ));

        List<String> roleCodes = sysUserRoleService.getRoleCodesByUserId(100L);

        assertEquals(List.of("SYSTEM_ADMIN", "AUDITOR"), roleCodes);
        verify(sysRoleService).getListByIds(List.of(2L, 1L), CommonStatusEnum.ENABLED.getCode());
    }

    @Test
    void getRoleCodesByUserIdShouldSkipRoleQueryWhenUserHasNoRoles() {
        when(sysUserRoleMapper.selectRoleIdsByUserId(100L)).thenReturn(List.of());

        List<String> roleCodes = sysUserRoleService.getRoleCodesByUserId(100L);

        assertEquals(List.of(), roleCodes);
        verify(sysRoleService, never()).getListByIds(any(), any());
    }

    private SysRole buildRole(Long id, String roleCode, Integer roleSort) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleSort(roleSort);
        role.setStatus(CommonStatusEnum.ENABLED.getCode());
        role.setIsDeleted(Boolean.FALSE);
        return role;
    }
}
