package com.vita.system.sysIpBlock.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.system.sysIpBlock.dto.SysIpBlockCreateDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockSearchDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockUpdateDto;
import com.vita.system.sysIpBlock.entity.SysIpBlock;
import com.vita.system.sysIpBlock.mapper.SysIpBlockMapper;
import com.vita.system.sysIpBlock.service.ISysIpBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.system.sysIpBlock.vo.SysIpBlockDetailVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockListVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.service.impl
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 Service 实现
 * @version 1.0
 */
@Service
public class SysIpBlockServiceImpl extends ServiceImpl<SysIpBlockMapper, SysIpBlock> implements ISysIpBlockService {

    @Resource
    private SysIpBlockMapper sysIpBlockMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysIpBlockCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysIpBlock data = new SysIpBlock();
        BeanUtils.copyProperties(createDto, data);
        sysIpBlockMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysIpBlockUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysIpBlock data = new SysIpBlock();
        BeanUtils.copyProperties(updateDto, data);
        sysIpBlockMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysIpBlockMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysIpBlockDetailVo get(Long id) {
        SysIpBlock data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysIpBlockDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysIpBlockListVo> list(SysIpBlockSearchDto searchDto) {
        List<SysIpBlock> dataList = sysIpBlockMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysIpBlockListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysIpBlockPageVo> page(SysIpBlockSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysIpBlockSearchDto();
        }
        PageResponse<SysIpBlock> selectPage = sysIpBlockMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysIpBlockPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysIpBlockPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapper<SysIpBlock> buildQueryWrapper(SysIpBlockSearchDto searchDto) {
        LambdaQueryWrapper<SysIpBlock> queryWrapper = new LambdaQueryWrapper<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysIpBlock::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysIpBlock getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysIpBlock data = sysIpBlockMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
