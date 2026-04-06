package com.vita.system.sysMenu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuSearchDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.vo.RouterVo;
import com.vita.system.sysMenu.vo.SysMenuDetailVo;
import com.vita.system.sysMenu.vo.SysMenuListVo;
import com.vita.system.sysMenu.vo.SysMenuPageVo;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.service
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 Service 接口
 */
public interface ISysMenuService extends IService<SysMenu> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysMenuCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysMenuUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysMenuDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysMenuListVo> list(SysMenuSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysMenuPageVo> page(SysMenuSearchDto searchDto);

    /**
     * 查询全部启用菜单。
     *
     * @return 菜单集合
     */
    List<SysMenu> getAllEnabledMenus();

    /**
     * 根据菜单ID和状态查询菜单集合。
     *
     * @param menuIds 菜单ID集合
     * @param status  菜单状态，可为空
     * @return 菜单集合
     */
    List<SysMenu> getListByIds(List<Long> menuIds, Byte status);

    /**
     * 根据用户ID获取其拥有的菜单列表。
     *
     * @param userId 用户 ID
     * @return 菜单列表
     */
    List<SysMenu> getMenusByUserId(Long userId);

    /**
     * 构建前端路由所需要的菜单
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    public List<RouterVo> buildMenus(List<SysMenu> menus);

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果 true 唯一 / false 不唯一
     */
    boolean checkMenuNameUnique(SysMenu menu);

    /**
     * 校验路由组合是否唯一
     *
     * @param menu 菜单信息
     * @return 结果 true 唯一 / false 不唯一
     */
    boolean checkRouteConfigUnique(SysMenu menu);
}
