package com.vita.system.sysRole.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.system.sysRole.dto.SysRoleCreateDto;
import com.vita.system.sysRole.dto.SysRoleSearchDto;
import com.vita.system.sysRole.dto.SysRoleUpdateDto;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.mapper.SysRoleMapper;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysRole.vo.SysRoleDetailVo;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRolePageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 Service 实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysRoleCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRole data = new SysRole();
        BeanUtils.copyProperties(createDto, data);
        sysRoleMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysRoleUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysRole data = new SysRole();
        BeanUtils.copyProperties(updateDto, data);
        sysRoleMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysRoleMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysRoleDetailVo get(Long id) {
        SysRole data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysRoleDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysRoleListVo> list(SysRoleSearchDto searchDto) {
        List<SysRole> dataList = sysRoleMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysRoleListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysRolePageVo> page(SysRoleSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysRoleSearchDto();
        }
        PageResponse<SysRole> selectPage = sysRoleMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysRolePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysRolePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 根据角色ID和状态列表查询角色信息。
     *
     * @param roleIds 角色ID列表
     * @param status  角色状态（可选）
     * @return 角色信息列表
     */
    @Override
    public List<SysRole> getListByIds(List<Long> roleIds, Byte status) {
        LambdaQueryWrapperX<SysRole> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.inIfPresent(SysRole::getId, roleIds)
                .eqIfPresent(SysRole::getStatus, status)
                .orderByAsc(SysRole::getRoleSort, SysRole::getId);
        List<SysRole> sysRoleList = sysRoleMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(sysRoleList)) {
            return sysRoleList;
        }
        return CollUtil.newArrayList();
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysRole> buildQueryWrapper(SysRoleSearchDto searchDto) {
        LambdaQueryWrapperX<SysRole> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null) {
            queryWrapper.eqIfPresent(SysRole::getId, searchDto.getId())
                    .eqIfPresent(SysRole::getStatus, searchDto.getStatus());
        }
        queryWrapper.orderByAsc(SysRole::getRoleSort, SysRole::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysRole getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRole data = sysRoleMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
