package com.vita.system.sysRole.service.impl;

import com.vita.constant.AuthConstants;
import com.vita.constant.Constants;
import com.vita.core.exception.ServiceException;
import com.vita.system.associate.sysRoleDeptScope.service.ISysRoleDeptScopeService;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysRole.dto.SysRoleCreateDto;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.mapper.SysRoleMapper;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRoleOptionVo;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private ISysUserRoleService sysUserRoleService;
    @Mock
    private ISysRoleMenuService sysRoleMenuService;
    @Mock
    private ISysRolePermissionService sysRolePermissionService;
    @Mock
    private ISysRoleDeptScopeService sysRoleDeptScopeService;
    @Mock
    private ISysUserService sysUserService;

    private SysRoleServiceImpl sysRoleService;

    @BeforeEach
    void setUp() {
        sysRoleService = new SysRoleServiceImpl();
        ReflectionTestUtils.setField(sysRoleService, "sysRoleMapper", sysRoleMapper);
        ReflectionTestUtils.setField(sysRoleService, "sysUserRoleService", sysUserRoleService);
        ReflectionTestUtils.setField(sysRoleService, "sysRoleMenuService", sysRoleMenuService);
        ReflectionTestUtils.setField(sysRoleService, "sysRolePermissionService", sysRolePermissionService);
        ReflectionTestUtils.setField(sysRoleService, "sysRoleDeptScopeService", sysRoleDeptScopeService);
        ReflectionTestUtils.setField(sysRoleService, "sysUserService", sysUserService);
    }

    @Test
    void createShouldRejectWhenRoleNameAlreadyExists() {
        when(sysRoleMapper.existsByRoleName("系统管理员", null)).thenReturn(true);

        SysRoleCreateDto createDto = buildCreateDto("系统管理员", "SYSTEM_ADMIN");

        ServiceException exception = assertThrows(ServiceException.class, () -> sysRoleService.create(createDto));

        assertEquals("角色名称已存在", exception.getMessage());
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    void createShouldRejectReservedRoleCode() {
        SysRoleCreateDto createDto = buildCreateDto("系统管理员", AuthConstants.SUPER_ADMIN_ROLE_CODE);

        ServiceException exception = assertThrows(ServiceException.class, () -> sysRoleService.create(createDto));

        assertEquals("角色编码 SUPER_ADMIN 为系统保留值", exception.getMessage());
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    void getListByUserIdShouldAssembleRolesByUser() {
        when(sysUserRoleService.getRoleIdsByUserId(100L)).thenReturn(List.of(2L, 1L));
        when(sysRoleMapper.selectListByIdsAndStatus(List.of(2L, 1L), null)).thenReturn(List.of(
                buildRole(1L, "系统管理员", "SYSTEM_ADMIN", Constants.ENABLED, Constants.DISABLED),
                buildRole(2L, "审计员", "AUDITOR", Constants.ENABLED, Constants.DISABLED)
        ));

        List<SysRoleListVo> roles = sysRoleService.getListByUserId(100L);

        assertEquals(2, roles.size());
        assertEquals("SYSTEM_ADMIN", roles.get(0).getRoleCode());
        assertEquals("AUDITOR", roles.get(1).getRoleCode());
    }

    @Test
    void getOptionListByUserIdShouldMarkSelectedAndDisabled() {
        when(sysUserRoleService.getRoleIdsByUserId(100L)).thenReturn(List.of(2L));
        when(sysRoleMapper.selectListByIdsAndStatus(null, null)).thenReturn(List.of(
                buildRole(1L, "普通角色", "COMMON", Constants.ENABLED, Constants.DISABLED),
                buildRole(2L, "系统角色", "SYSTEM", Constants.ENABLED, Constants.ENABLED),
                buildRole(3L, "禁用角色", "DISABLED_ROLE", Constants.DISABLED, Constants.DISABLED)
        ));

        List<SysRoleOptionVo> options = sysRoleService.getOptionListByUserId(100L);

        assertEquals(3, options.size());
        assertFalse(options.get(0).isSelected());
        assertFalse(options.get(0).isDisabled());
        assertTrue(options.get(1).isSelected());
        assertTrue(options.get(1).isDisabled());
        assertTrue(options.get(2).isDisabled());
    }

    @Test
    void deleteBatchShouldRejectProtectedRole() {
        when(sysRoleMapper.selectById(1L)).thenReturn(buildRole(1L, "系统角色", "SYSTEM", Constants.ENABLED, Constants.ENABLED));

        ServiceException exception = assertThrows(ServiceException.class, () -> sysRoleService.deleteBatch(List.of(1L)));

        assertEquals("系统内置角色不允许操作", exception.getMessage());
        verify(sysUserRoleService, never()).removeByRoleIds(any());
    }

    @Test
    void deleteBatchShouldCascadeRemoveAssociations() {
        when(sysRoleMapper.selectById(1L)).thenReturn(buildRole(1L, "普通角色A", "ROLE_A", Constants.ENABLED, Constants.DISABLED));
        when(sysRoleMapper.selectById(2L)).thenReturn(buildRole(2L, "普通角色B", "ROLE_B", Constants.ENABLED, Constants.DISABLED));

        sysRoleService.deleteBatch(List.of(1L, 2L, 2L));

        verify(sysUserRoleService).removeByRoleIds(List.of(1L, 2L));
        verify(sysRoleMenuService).removeByRoleIds(List.of(1L, 2L));
        verify(sysRolePermissionService).removeByRoleIds(List.of(1L, 2L));
        verify(sysRoleDeptScopeService).removeByRoleIds(List.of(1L, 2L));
        verify(sysRoleMapper).deleteById(1L);
        verify(sysRoleMapper).deleteById(2L);
    }

    @Test
    void authUsersShouldValidateUsersThenAssignRole() {
        when(sysRoleMapper.selectById(10L)).thenReturn(buildRole(10L, "普通角色", "COMMON", Constants.ENABLED, Constants.DISABLED));
        when(sysUserService.listByIds(List.of(101L, 102L))).thenReturn(List.of(buildUser(101L), buildUser(102L)));

        sysRoleService.authUsers(10L, List.of(101L, 102L, 101L));

        verify(sysUserRoleService).assignRoleToUsers(10L, List.of(101L, 102L));
    }

    private SysRoleCreateDto buildCreateDto(String roleName, String roleCode) {
        SysRoleCreateDto createDto = new SysRoleCreateDto();
        createDto.setRoleName(roleName);
        createDto.setRoleCode(roleCode);
        createDto.setRoleSort(1);
        createDto.setStatus(Constants.ENABLED);
        createDto.setDataScopeType("SELF");
        return createDto;
    }

    private SysRole buildRole(Long id, String roleName, String roleCode, Byte status, Byte isSystem) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setRoleSort(1);
        role.setStatus(status);
        role.setIsSystem(isSystem);
        role.setDataScopeType("SELF");
        role.setIsDeleted(Boolean.FALSE);
        return role;
    }

    private SysUser buildUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        return user;
    }
}
