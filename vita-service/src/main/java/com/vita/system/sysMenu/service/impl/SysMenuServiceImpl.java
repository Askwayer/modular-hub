package com.vita.system.sysMenu.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuSearchDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.mapper.SysMenuMapper;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysMenu.vo.SysMenuDetailVo;
import com.vita.system.sysMenu.vo.SysMenuListVo;
import com.vita.system.sysMenu.vo.SysMenuPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 Service 实现
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    @Resource
    private SysMenuMapper sysMenuMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysMenuCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysMenu data = new SysMenu();
        BeanUtils.copyProperties(createDto, data);
        sysMenuMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysMenuUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysMenu data = new SysMenu();
        BeanUtils.copyProperties(updateDto, data);
        sysMenuMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysMenuMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysMenuDetailVo get(Long id) {
        SysMenu data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysMenuDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysMenuListVo> list(SysMenuSearchDto searchDto) {
        List<SysMenu> dataList = sysMenuMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysMenuListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysMenuPageVo> page(SysMenuSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysMenuSearchDto();
        }
        PageResponse<SysMenu> selectPage = sysMenuMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysMenuPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysMenuPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 查询全部启用菜单。
     *
     * @return 菜单集合
     */
    @Override
    public List<SysMenu> getAllEnabledMenus() {
        List<SysMenu> menuList = sysMenuMapper.selectAllEnabledMenus();
        if (CollUtil.isEmpty(menuList)) {
            return CollUtil.newArrayList();
        }
        return menuList;
    }

    /**
     * 根据菜单ID和状态查询菜单集合。
     *
     * @param menuIds 菜单ID集合
     * @param status  菜单状态，可为空
     * @return 菜单集合
     */
    @Override
    public List<SysMenu> getListByIds(List<Long> menuIds, Byte status) {
        if (CollUtil.isEmpty(menuIds)) {
            return CollUtil.newArrayList();
        }
        List<SysMenu> menuList = sysMenuMapper.selectList(new LambdaQueryWrapperX<SysMenu>()
                .inIfPresent(SysMenu::getId, menuIds)
                .eqIfPresent(SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getSortNo, SysMenu::getId));
        if (CollUtil.isEmpty(menuList)) {
            return CollUtil.newArrayList();
        }
        return menuList;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysMenu> buildQueryWrapper(SysMenuSearchDto searchDto) {
        LambdaQueryWrapperX<SysMenu> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysMenu::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysMenu getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysMenu data = sysMenuMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
