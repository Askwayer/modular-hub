package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowSkipCreateDto;
import com.vita.workflow.flow.dto.FlowSkipSearchDto;
import com.vita.workflow.flow.dto.FlowSkipUpdateDto;
import com.vita.workflow.flow.entity.FlowSkip;
import com.vita.workflow.flow.mapper.FlowSkipMapper;
import com.vita.workflow.flow.service.IFlowSkipService;
import com.vita.workflow.flow.vo.FlowSkipDetailVo;
import com.vita.workflow.flow.vo.FlowSkipListVo;
import com.vita.workflow.flow.vo.FlowSkipPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 节点跳转关联表 Service 实现
 * @version 1.0
 */
@Service
public class FlowSkipServiceImpl extends ServiceImpl<FlowSkipMapper, FlowSkip> implements IFlowSkipService {

    @Resource
    private FlowSkipMapper flowSkipMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowSkipCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowSkip data = new FlowSkip();
        BeanUtils.copyProperties(createDto, data);
        flowSkipMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowSkipUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowSkip data = new FlowSkip();
        BeanUtils.copyProperties(updateDto, data);
        flowSkipMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowSkipMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowSkipDetailVo get(Long id) {
        FlowSkip data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowSkipDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowSkipListVo> list(FlowSkipSearchDto searchDto) {
        List<FlowSkip> dataList = flowSkipMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowSkipListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowSkipPageVo> page(FlowSkipSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowSkipSearchDto();
        }
        PageResponse<FlowSkip> selectPage = flowSkipMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowSkipPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowSkipPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowSkip> buildQueryWrapper(FlowSkipSearchDto searchDto) {
        LambdaQueryWrapperX<FlowSkip> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowSkip::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowSkip getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowSkip data = flowSkipMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
