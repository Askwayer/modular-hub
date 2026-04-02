package com.vita.system.sysPermission.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.system.sysPermission.dto.SysPermissionCreateDto;
import com.vita.system.sysPermission.dto.SysPermissionSearchDto;
import com.vita.system.sysPermission.dto.SysPermissionUpdateDto;
import com.vita.system.sysPermission.entity.SysPermission;
import com.vita.system.sysPermission.mapper.SysPermissionMapper;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.system.sysPermission.vo.SysPermissionDetailVo;
import com.vita.system.sysPermission.vo.SysPermissionListVo;
import com.vita.system.sysPermission.vo.SysPermissionPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 Service 实现
 * @version 1.0
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements ISysPermissionService {

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysPermissionCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysPermission data = new SysPermission();
        BeanUtils.copyProperties(createDto, data);
        sysPermissionMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysPermissionUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysPermission data = new SysPermission();
        BeanUtils.copyProperties(updateDto, data);
        sysPermissionMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysPermissionMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysPermissionDetailVo get(Long id) {
        SysPermission data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysPermissionDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysPermissionListVo> list(SysPermissionSearchDto searchDto) {
        List<SysPermission> dataList = sysPermissionMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysPermissionListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysPermissionPageVo> page(SysPermissionSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysPermissionSearchDto();
        }
        PageResponse<SysPermission> selectPage = sysPermissionMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysPermissionPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysPermissionPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 根据权限ID和状态查询权限码集合。
     *
     * @param permissionIds 权限ID集合
     * @param status 权限状态，可为空
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionCodesByIds(List<Long> permissionIds, Byte status) {
        if (CollUtil.isEmpty(permissionIds)) {
            return CollUtil.newArrayList();
        }
        return sysPermissionMapper.selectList(new LambdaQueryWrapperX<SysPermission>()
                        .inIfPresent(SysPermission::getId, permissionIds)
                        .eqIfPresent(SysPermission::getStatus, status)
                        .orderByAsc(SysPermission::getSortNo, SysPermission::getId))
                .stream()
                .map(permission -> StrUtil.isNotBlank(permission.getAuthTag())
                        ? permission.getAuthTag()
                        : permission.getPermissionCode())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysPermission> buildQueryWrapper(SysPermissionSearchDto searchDto) {
        LambdaQueryWrapperX<SysPermission> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysPermission::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysPermission getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysPermission data = sysPermissionMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
