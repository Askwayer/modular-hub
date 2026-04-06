package com.vita.system.sysMenu.service.impl;

import com.vita.constant.Constants;
import com.vita.core.exception.ServiceException;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.mapper.SysMenuMapper;
import com.vita.system.sysMenu.vo.RouterVo;
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
    @Mock
    private ISysUserService sysUserService;
    @Mock
    private ISysUserRoleService sysUserRoleService;
    @Mock
    private ISysRoleMenuService sysRoleMenuService;

    private SysMenuServiceImpl sysMenuService;

    @BeforeEach
    void setUp() {
        sysMenuService = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(sysMenuService, "sysMenuMapper", sysMenuMapper);
        ReflectionTestUtils.setField(sysMenuService, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(sysMenuService, "sysUserRoleService", sysUserRoleService);
        ReflectionTestUtils.setField(sysMenuService, "sysRoleMenuService", sysRoleMenuService);
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

    @Test
    void getMenusByUserIdShouldReturnRoleMenusForNormalUser() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setIsSuperAdmin(Constants.DISABLED);
        when(sysUserService.getActiveById(2L)).thenReturn(user);
        when(sysUserRoleService.getRoleIdsByUserId(2L)).thenReturn(List.of(11L));
        when(sysRoleMenuService.getMenuIdsByRoleIds(List.of(11L))).thenReturn(List.of(1001L));
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(buildMenu(1001L, 0L, "用户管理", "SysUser", "/system/sysUser", 1)));

        List<SysMenu> menus = sysMenuService.getMenusByUserId(2L);

        assertEquals(1, menus.size());
        assertEquals("/system/sysUser", menus.get(0).getRouteLink());
    }

    @Test
    void buildMenusShouldBuildVisibleRouteTree() {
        List<RouterVo> routers = sysMenuService.buildMenus(List.of(
                buildMenu(1000L, 0L, "系统管理", "System", "/system", 1),
                buildMenu(1001L, 1000L, "用户管理", "SysUser", "/system/sysUser", 1),
                buildMenu(1002L, 1000L, "隐藏菜单", "HiddenMenu", "/hidden", 0)
        ));

        assertEquals(1, routers.size());
        assertEquals("/system", routers.get(0).getRouteLink());
        assertEquals(1, routers.get(0).getChildren().size());
        assertEquals("/system/sysUser", routers.get(0).getChildren().get(0).getRouteLink());
    }

    private SysMenu buildMenu(Long id, Long parentId, String menuName, String routeName, String routeLink, Integer visible) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        menu.setRouteName(routeName);
        menu.setRouteLink(routeLink);
        menu.setComponentPath("Layout");
        menu.setVisible(visible.byteValue());
        menu.setAlwaysShow(Constants.DISABLED);
        menu.setStatus(Constants.ENABLED);
        return menu;
    }
}
