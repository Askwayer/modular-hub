package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowDefinitionCreateDto;
import com.vita.workflow.flow.dto.FlowDefinitionSearchDto;
import com.vita.workflow.flow.dto.FlowDefinitionUpdateDto;
import com.vita.workflow.flow.entity.FlowDefinition;
import com.vita.workflow.flow.mapper.FlowDefinitionMapper;
import com.vita.workflow.flow.service.IFlowDefinitionService;
import com.vita.workflow.flow.vo.FlowDefinitionDetailVo;
import com.vita.workflow.flow.vo.FlowDefinitionListVo;
import com.vita.workflow.flow.vo.FlowDefinitionPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程定义表 Service 实现
 * @version 1.0
 */
@Service
public class FlowDefinitionServiceImpl extends ServiceImpl<FlowDefinitionMapper, FlowDefinition> implements IFlowDefinitionService {

    @Resource
    private FlowDefinitionMapper flowDefinitionMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowDefinitionCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowDefinition data = new FlowDefinition();
        BeanUtils.copyProperties(createDto, data);
        flowDefinitionMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowDefinitionUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowDefinition data = new FlowDefinition();
        BeanUtils.copyProperties(updateDto, data);
        flowDefinitionMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowDefinitionMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowDefinitionDetailVo get(Long id) {
        FlowDefinition data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowDefinitionDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowDefinitionListVo> list(FlowDefinitionSearchDto searchDto) {
        List<FlowDefinition> dataList = flowDefinitionMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowDefinitionListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowDefinitionPageVo> page(FlowDefinitionSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowDefinitionSearchDto();
        }
        PageResponse<FlowDefinition> selectPage = flowDefinitionMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowDefinitionPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowDefinitionPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowDefinition> buildQueryWrapper(FlowDefinitionSearchDto searchDto) {
        LambdaQueryWrapperX<FlowDefinition> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowDefinition::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowDefinition getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowDefinition data = flowDefinitionMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
