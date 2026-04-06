package com.vita.system.associate.sysRolePermission.service.impl;

import com.vita.core.exception.ServiceException;
import com.vita.system.associate.sysRolePermission.entity.SysRolePermission;
import com.vita.system.associate.sysRolePermission.mapper.SysRolePermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
        ReflectionTestUtils.setField(sysRolePermissionService, "baseMapper", sysRolePermissionMapper);
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

    @Test
    void getPermissionIdsByRoleIdShouldRejectNullRoleId() {
        assertThrows(ServiceException.class, () -> sysRolePermissionService.getPermissionIdsByRoleId(null));
    }

    @Test
    void removeByPermissionIdsShouldDeleteAssociationRows() {
        sysRolePermissionService.removeByPermissionIds(List.of(100L, 200L));

        verify(sysRolePermissionMapper).delete(any());
    }

    @Test
    void removeByRoleIdAndPermissionIdsShouldDeleteAssociationRows() {
        sysRolePermissionService.removeByRoleIdAndPermissionIds(10L, List.of(100L, 200L));

        verify(sysRolePermissionMapper).delete(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void assignPermissionsToRoleShouldInsertOnlyMissingAssociations() {
        SysRolePermissionServiceImpl spyService = spy(sysRolePermissionService);
        ReflectionTestUtils.setField(spyService, "sysRolePermissionMapper", sysRolePermissionMapper);
        ReflectionTestUtils.setField(spyService, "baseMapper", sysRolePermissionMapper);
        when(sysRolePermissionMapper.selectList(any())).thenReturn(List.of(buildRolePermission(1L, 10L, 100L)));
        doReturn(true).when(spyService).saveBatch(anyCollection());

        spyService.assignPermissionsToRole(10L, List.of(100L, 200L, 200L));

        ArgumentCaptor<List<SysRolePermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyService).saveBatch(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(10L, captor.getValue().get(0).getRoleId());
        assertEquals(200L, captor.getValue().get(0).getPermissionId());
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
