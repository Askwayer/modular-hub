package com.vita.system.associate.sysRolePermission.service.impl;

import com.vita.system.associate.sysRolePermission.entity.SysRolePermission;
import com.vita.system.associate.sysRolePermission.mapper.SysRolePermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRolePermissionServiceImplTest {

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    private SysRolePermissionServiceImpl sysRolePermissionService;

    @BeforeEach
    void setUp() {
        sysRolePermissionService = new SysRolePermissionServiceImpl();
        ReflectionTestUtils.setField(sysRolePermissionService, "sysRolePermissionMapper", sysRolePermissionMapper);
    }

    @Test
    void getPermissionIdsByRoleIdsShouldSkipMapperWhenRoleIdsEmpty() {
        assertEquals(List.of(), sysRolePermissionService.getPermissionIdsByRoleIds(List.of()));
        verify(sysRolePermissionMapper, never()).selectList(any());
    }

    @Test
    void getPermissionIdsByRoleIdsShouldReturnDistinctPermissionIds() {
        when(sysRolePermissionMapper.selectList(any())).thenReturn(List.of(
                buildRolePermission(1L, 10L, 100L),
                buildRolePermission(2L, 20L, 200L),
                buildRolePermission(3L, 30L, 100L)
        ));

        List<Long> permissionIds = sysRolePermissionService.getPermissionIdsByRoleIds(List.of(10L, 20L));

        assertEquals(List.of(100L, 200L), permissionIds);
    }

    private SysRolePermission buildRolePermission(Long id, Long roleId, Long permissionId) {
        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setId(id);
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermission.setIsDeleted(Boolean.FALSE);
        return rolePermission;
    }
}
