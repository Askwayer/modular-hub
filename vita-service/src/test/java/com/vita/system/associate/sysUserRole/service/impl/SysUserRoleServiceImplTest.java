package com.vita.system.associate.sysUserRole.service.impl;

import com.vita.core.exception.ServiceException;
import com.vita.system.associate.sysUserRole.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserRoleServiceImplTest {

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    private SysUserRoleServiceImpl sysUserRoleService;

    @BeforeEach
    void setUp() {
        sysUserRoleService = new SysUserRoleServiceImpl();
        ReflectionTestUtils.setField(sysUserRoleService, "sysUserRoleMapper", sysUserRoleMapper);
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
}
