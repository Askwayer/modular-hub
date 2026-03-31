package com.vita.system.associate.sysRolePermission.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionCreateDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionSearchDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionUpdateDto;
import com.vita.system.associate.sysRolePermission.entity.SysRolePermission;
import com.vita.system.associate.sysRolePermission.mapper.SysRolePermissionMapper;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionDetailVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionListVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRolePermission.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:40:54
 * @Description: 角色权限关联表 Service 实现
 * @version 1.0
 */
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> implements ISysRolePermissionService {

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysRolePermissionCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRolePermission data = new SysRolePermission();
        BeanUtils.copyProperties(createDto, data);
        sysRolePermissionMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysRolePermissionUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysRolePermission data = new SysRolePermission();
        BeanUtils.copyProperties(updateDto, data);
        sysRolePermissionMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysRolePermissionMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysRolePermissionDetailVo get(Long id) {
        SysRolePermission data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysRolePermissionDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysRolePermissionListVo> list(SysRolePermissionSearchDto searchDto) {
        List<SysRolePermission> dataList = sysRolePermissionMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysRolePermissionListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysRolePermissionPageVo> page(SysRolePermissionSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysRolePermissionSearchDto();
        }
        PageResponse<SysRolePermission> selectPage = sysRolePermissionMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysRolePermissionPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysRolePermissionPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapper<SysRolePermission> buildQueryWrapper(SysRolePermissionSearchDto searchDto) {
        LambdaQueryWrapper<SysRolePermission> queryWrapper = new LambdaQueryWrapper<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysRolePermission::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysRolePermission getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRolePermission data = sysRolePermissionMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
