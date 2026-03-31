package com.vita.system.associate.sysUserRole.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleCreateDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleSearchDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleUpdateDto;
import com.vita.system.associate.sysUserRole.entity.SysUserRole;
import com.vita.system.associate.sysUserRole.mapper.SysUserRoleMapper;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleDetailVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleListVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRolePageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysUserRole.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:39:20
 * @Description: 用户角色关联表 Service 实现
 * @version 1.0
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements ISysUserRoleService {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysUserRoleCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysUserRole data = new SysUserRole();
        BeanUtils.copyProperties(createDto, data);
        sysUserRoleMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysUserRoleUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysUserRole data = new SysUserRole();
        BeanUtils.copyProperties(updateDto, data);
        sysUserRoleMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysUserRoleMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysUserRoleDetailVo get(Long id) {
        SysUserRole data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysUserRoleDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysUserRoleListVo> list(SysUserRoleSearchDto searchDto) {
        List<SysUserRole> dataList = sysUserRoleMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysUserRoleListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysUserRolePageVo> page(SysUserRoleSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysUserRoleSearchDto();
        }
        PageResponse<SysUserRole> selectPage = sysUserRoleMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysUserRolePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysUserRolePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapper<SysUserRole> buildQueryWrapper(SysUserRoleSearchDto searchDto) {
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysUserRole::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysUserRole getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysUserRole data = sysUserRoleMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
