package com.vita.system.sysMenu.service.impl;

import com.vita.core.exception.ServiceException;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.mapper.SysMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    private SysMenuServiceImpl sysMenuService;

    @BeforeEach
    void setUp() {
        sysMenuService = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(sysMenuService, "sysMenuMapper", sysMenuMapper);
    }

    @Test
    void createShouldRejectWhenSiblingMenuNameAlreadyExists() {
        when(sysMenuMapper.selectCount(any())).thenReturn(1L);

        SysMenuCreateDto createDto = new SysMenuCreateDto();
        createDto.setParentId(1000L);
        createDto.setMenuName("用户管理");
        createDto.setRouteName("SysUser");
        createDto.setRouteLink("/system/sysUser");

        ServiceException exception = assertThrows(ServiceException.class, () -> sysMenuService.create(createDto));

        assertEquals("同级菜单名称已存在", exception.getMessage());
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    void updateShouldRejectWhenRouteConfigAlreadyExists() {
        SysMenu existed = new SysMenu();
        existed.setId(10L);
        when(sysMenuMapper.selectById(10L)).thenReturn(existed);
        when(sysMenuMapper.selectCount(any())).thenReturn(0L, 1L);

        SysMenuUpdateDto updateDto = new SysMenuUpdateDto();
        updateDto.setId(10L);
        updateDto.setParentId(1000L);
        updateDto.setMenuName("菜单管理");
        updateDto.setRouteName("SysMenu");
        updateDto.setRouteLink("/system/sysMenu");

        ServiceException exception = assertThrows(ServiceException.class, () -> sysMenuService.update(updateDto));

        assertEquals("路由名称或路由地址已存在", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    void checkRouteConfigUniqueShouldReturnTrueWhenRouteConfigBlank() {
        SysMenu menu = new SysMenu();
        menu.setId(1L);
        menu.setMenuName("系统管理");

        assertTrue(sysMenuService.checkRouteConfigUnique(menu));
        verify(sysMenuMapper, never()).selectCount(any());
    }
}
