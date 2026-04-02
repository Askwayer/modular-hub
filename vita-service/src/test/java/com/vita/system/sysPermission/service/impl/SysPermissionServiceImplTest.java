package com.vita.system.sysPermission.service.impl;

import com.vita.constant.Constants;
import com.vita.system.sysPermission.entity.SysPermission;
import com.vita.system.sysPermission.mapper.SysPermissionMapper;
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
class SysPermissionServiceImplTest {

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    private SysPermissionServiceImpl sysPermissionService;

    @BeforeEach
    void setUp() {
        sysPermissionService = new SysPermissionServiceImpl();
        ReflectionTestUtils.setField(sysPermissionService, "sysPermissionMapper", sysPermissionMapper);
    }

    @Test
    void getPermissionCodesByIdsShouldSkipMapperWhenPermissionIdsEmpty() {
        assertEquals(List.of(), sysPermissionService.getPermissionCodesByIds(List.of(), Constants.ENABLED));
        verify(sysPermissionMapper, never()).selectList(any());
    }

    @Test
    void getPermissionCodesByIdsShouldPreferAuthTagAndFallbackToPermissionCode() {
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(
                buildPermission(1L, "system:user:view", "system:user:auth", 1),
                buildPermission(2L, "system:user:edit", "", 2),
                buildPermission(3L, "system:user:view", "system:user:auth", 3)
        ));

        List<String> permissionCodes = sysPermissionService.getPermissionCodesByIds(List.of(1L, 2L, 3L), Constants.ENABLED);

        assertEquals(List.of("system:user:auth", "system:user:edit"), permissionCodes);
    }

    private SysPermission buildPermission(Long id, String permissionCode, String authTag, Integer sortNo) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionCode(permissionCode);
        permission.setAuthTag(authTag);
        permission.setSortNo(sortNo);
        permission.setStatus(Constants.ENABLED);
        permission.setIsDeleted(Boolean.FALSE);
        return permission;
    }
}
