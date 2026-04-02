package com.vita.system.associate.sysRoleMenu.service.impl;

import com.vita.system.associate.sysRoleMenu.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRoleMenuServiceImplTest {

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    private SysRoleMenuServiceImpl sysRoleMenuService;

    @BeforeEach
    void setUp() {
        sysRoleMenuService = new SysRoleMenuServiceImpl();
        ReflectionTestUtils.setField(sysRoleMenuService, "sysRoleMenuMapper", sysRoleMenuMapper);
    }

    @Test
    void getMenuIdsByRoleIdsShouldSkipMapperWhenRoleIdsEmpty() {
        assertEquals(List.of(), sysRoleMenuService.getMenuIdsByRoleIds(List.of()));
        verify(sysRoleMenuMapper, never()).selectMenuIdsByRoleIds(List.of());
    }

    @Test
    void getMenuIdsByRoleIdsShouldReturnMenuIdsFromMapper() {
        when(sysRoleMenuMapper.selectMenuIdsByRoleIds(List.of(10L, 20L))).thenReturn(List.of(100L, 200L));

        List<Long> menuIds = sysRoleMenuService.getMenuIdsByRoleIds(List.of(10L, 20L));

        assertEquals(List.of(100L, 200L), menuIds);
        verify(sysRoleMenuMapper).selectMenuIdsByRoleIds(List.of(10L, 20L));
    }
}
