package com.vita.system.sysPermission.service.impl;

import com.vita.constant.Constants;
import com.vita.core.exception.ServiceException;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.sysPermission.dto.SysPermissionCreateDto;
import com.vita.system.sysPermission.dto.SysPermissionUpdateDto;
import com.vita.system.sysPermission.entity.SysPermission;
import com.vita.system.sysPermission.mapper.SysPermissionMapper;
import com.vita.system.sysPermission.vo.SysPermissionListVo;
import com.vita.system.sysPermission.vo.SysPermissionOptionVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysPermissionServiceImplTest {

    @Mock
    private SysPermissionMapper sysPermissionMapper;
    @Mock
    private ISysRolePermissionService sysRolePermissionService;

    private SysPermissionServiceImpl sysPermissionService;

    @BeforeEach
    void setUp() {
        sysPermissionService = new SysPermissionServiceImpl();
        ReflectionTestUtils.setField(sysPermissionService, "sysPermissionMapper", sysPermissionMapper);
        ReflectionTestUtils.setField(sysPermissionService, "sysRolePermissionService", sysRolePermissionService);
    }

    @Test
    void getPermissionCodesByIdsShouldSkipMapperWhenPermissionIdsEmpty() {
        assertEquals(List.of(), sysPermissionService.getPermissionCodesByIds(List.of(), Constants.ENABLED));
        verify(sysPermissionMapper, never()).selectListByIdsAndStatus(any(), any());
    }

    @Test
    void getPermissionCodesByIdsShouldPreferAuthTagAndFallbackToPermissionCode() {
        when(sysPermissionMapper.selectListByIdsAndStatus(List.of(1L, 2L, 3L), Constants.ENABLED)).thenReturn(List.of(
                buildPermission(1L, "system:user:view", "system:user:auth", 1, Constants.ENABLED, Constants.DISABLED),
                buildPermission(2L, "system:user:update", "", 2, Constants.ENABLED, Constants.DISABLED),
                buildPermission(3L, "system:user:view", "system:user:auth", 3, Constants.ENABLED, Constants.DISABLED)
        ));

        List<String> permissionCodes = sysPermissionService.getPermissionCodesByIds(List.of(1L, 2L, 3L), Constants.ENABLED);

        assertEquals(List.of("system:user:auth", "system:user:update"), permissionCodes);
    }

    @Test
    void createShouldDefaultAuthTagToPermissionCode() {
        when(sysPermissionMapper.existsByPermissionCode("system:permission:view", null)).thenReturn(false);
        when(sysPermissionMapper.existsByAuthTag("system:permission:view", null)).thenReturn(false);

        sysPermissionService.create(buildCreateDto("权限查看", "system:permission:view", null));

        ArgumentCaptor<SysPermission> captor = ArgumentCaptor.forClass(SysPermission.class);
        verify(sysPermissionMapper).insert(captor.capture());
        assertEquals("system:permission:view", captor.getValue().getAuthTag());
    }

    @Test
    void getListByRoleIdShouldAssemblePermissionsByRoleId() {
        when(sysRolePermissionService.getPermissionIdsByRoleId(10L)).thenReturn(List.of(2L, 1L));
        when(sysPermissionMapper.selectListByIdsAndStatus(List.of(2L, 1L), null)).thenReturn(List.of(
                buildPermission(1L, "system:user:view", "system:user:view", 1, Constants.ENABLED, Constants.DISABLED),
                buildPermission(2L, "system:user:update", "system:user:update", 2, Constants.ENABLED, Constants.DISABLED)
        ));

        List<SysPermissionListVo> permissions = sysPermissionService.getListByRoleId(10L);

        assertEquals(2, permissions.size());
        assertEquals("system:user:view", permissions.get(0).getPermissionCode());
        assertEquals("system:user:update", permissions.get(1).getPermissionCode());
    }

    @Test
    void getOptionListByRoleIdShouldMarkSelectedAndDisabled() {
        when(sysRolePermissionService.getPermissionIdsByRoleId(10L)).thenReturn(List.of(2L));
        when(sysPermissionMapper.selectListByIdsAndStatus(null, null)).thenReturn(List.of(
                buildPermission(1L, "system:user:view", "system:user:view", 1, Constants.ENABLED, Constants.DISABLED),
                buildPermission(2L, "system:permission:view", "system:permission:view", 2, Constants.ENABLED, Constants.ENABLED),
                buildPermission(3L, "system:user:update", "system:user:update", 3, Constants.DISABLED, Constants.DISABLED)
        ));

        List<SysPermissionOptionVo> options = sysPermissionService.getOptionListByRoleId(10L);

        assertEquals(3, options.size());
        assertFalse(options.get(0).isSelected());
        assertFalse(options.get(0).isDisabled());
        assertTrue(options.get(1).isSelected());
        assertTrue(options.get(1).isDisabled());
        assertTrue(options.get(2).isDisabled());
    }

    @Test
    void checkPermissionCodeUniqueShouldReturnFalseWhenCodeExists() {
        SysPermission permission = new SysPermission();
        permission.setPermissionCode("system:user:view");
        when(sysPermissionMapper.existsByPermissionCode("system:user:view", null)).thenReturn(true);

        assertFalse(sysPermissionService.checkPermissionCodeUnique(permission));
    }

    @Test
    void checkAuthTagUniqueShouldReturnFalseWhenAuthTagExists() {
        SysPermission permission = new SysPermission();
        permission.setAuthTag("system:user:view");
        when(sysPermissionMapper.existsByAuthTag("system:user:view", null)).thenReturn(true);

        assertFalse(sysPermissionService.checkAuthTagUnique(permission));
    }

    @Test
    void deleteBatchShouldRejectProtectedPermission() {
        when(sysPermissionMapper.selectById(1L)).thenReturn(
                buildPermission(1L, "system:user:view", "system:user:view", 1, Constants.ENABLED, Constants.ENABLED));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> sysPermissionService.deleteBatch(List.of(1L)));

        assertEquals("系统内置权限不允许操作", exception.getMessage());
        verify(sysRolePermissionService, never()).removeByPermissionIds(any());
    }

    @Test
    void deleteBatchShouldCascadeRemoveRoleAssociations() {
        when(sysPermissionMapper.selectById(1L)).thenReturn(
                buildPermission(1L, "system:user:view", "system:user:view", 1, Constants.ENABLED, Constants.DISABLED));
        when(sysPermissionMapper.selectById(2L)).thenReturn(
                buildPermission(2L, "system:user:update", "system:user:update", 2, Constants.ENABLED, Constants.DISABLED));

        sysPermissionService.deleteBatch(List.of(1L, 2L, 2L));

        verify(sysRolePermissionService).removeByPermissionIds(List.of(1L, 2L));
        verify(sysPermissionMapper).deleteById(1L);
        verify(sysPermissionMapper).deleteById(2L);
    }

    @Test
    void deleteShouldCascadeRemoveRoleAssociationsLikeDeleteBatch() {
        when(sysPermissionMapper.selectById(1L)).thenReturn(
                buildPermission(1L, "system:user:view", "system:user:view", 1, Constants.ENABLED, Constants.DISABLED));

        sysPermissionService.delete(1L);

        verify(sysRolePermissionService).removeByPermissionIds(List.of(1L));
        verify(sysPermissionMapper).deleteById(1L);
    }

    @Test
    void updateShouldRejectProtectedPermissionCodeChange() {
        when(sysPermissionMapper.selectById(1L)).thenReturn(
                buildPermission(1L, "system:user:view", "system:user:view", 1, Constants.ENABLED, Constants.ENABLED));
        when(sysPermissionMapper.existsByPermissionCode("system:user:delete", 1L)).thenReturn(false);
        when(sysPermissionMapper.existsByAuthTag("system:user:delete", 1L)).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> sysPermissionService.update(buildUpdateDto(1L, "权限删除", "system:user:delete", "system:user:delete")));

        assertEquals("系统内置权限不允许修改权限编码或鉴权标识", exception.getMessage());
    }

    private SysPermissionCreateDto buildCreateDto(String permissionName, String permissionCode, String authTag) {
        SysPermissionCreateDto createDto = new SysPermissionCreateDto();
        createDto.setPermissionName(permissionName);
        createDto.setPermissionCode(permissionCode);
        createDto.setPermissionType("MENU_ACTION");
        createDto.setAuthTag(authTag);
        createDto.setSortNo(1);
        createDto.setStatus(Constants.ENABLED);
        createDto.setIsSystem(Constants.DISABLED);
        return createDto;
    }

    private SysPermissionUpdateDto buildUpdateDto(Long id, String permissionName, String permissionCode, String authTag) {
        SysPermissionUpdateDto updateDto = new SysPermissionUpdateDto();
        updateDto.setId(id);
        updateDto.setPermissionName(permissionName);
        updateDto.setPermissionCode(permissionCode);
        updateDto.setPermissionType("MENU_ACTION");
        updateDto.setAuthTag(authTag);
        updateDto.setSortNo(1);
        updateDto.setStatus(Constants.ENABLED);
        updateDto.setIsSystem(Constants.ENABLED);
        return updateDto;
    }

    private SysPermission buildPermission(Long id, String permissionCode, String authTag, Integer sortNo, Byte status, Byte isSystem) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionName("权限" + id);
        permission.setPermissionCode(permissionCode);
        permission.setPermissionType("MENU_ACTION");
        permission.setAuthTag(authTag);
        permission.setSortNo(sortNo);
        permission.setStatus(status);
        permission.setIsSystem(isSystem);
        permission.setIsDeleted(Boolean.FALSE);
        return permission;
    }
}
