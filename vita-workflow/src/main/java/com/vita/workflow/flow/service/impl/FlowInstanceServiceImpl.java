package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowInstanceCreateDto;
import com.vita.workflow.flow.dto.FlowInstanceSearchDto;
import com.vita.workflow.flow.dto.FlowInstanceUpdateDto;
import com.vita.workflow.flow.entity.FlowInstance;
import com.vita.workflow.flow.mapper.FlowInstanceMapper;
import com.vita.workflow.flow.service.IFlowInstanceService;
import com.vita.workflow.flow.vo.FlowInstanceDetailVo;
import com.vita.workflow.flow.vo.FlowInstanceListVo;
import com.vita.workflow.flow.vo.FlowInstancePageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程实例表 Service 实现
 * @version 1.0
 */
@Service
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance> implements IFlowInstanceService {

    @Resource
    private FlowInstanceMapper flowInstanceMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowInstanceCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowInstance data = new FlowInstance();
        BeanUtils.copyProperties(createDto, data);
        flowInstanceMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowInstanceUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowInstance data = new FlowInstance();
        BeanUtils.copyProperties(updateDto, data);
        flowInstanceMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowInstanceMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowInstanceDetailVo get(Long id) {
        FlowInstance data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowInstanceDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowInstanceListVo> list(FlowInstanceSearchDto searchDto) {
        List<FlowInstance> dataList = flowInstanceMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowInstanceListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowInstancePageVo> page(FlowInstanceSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowInstanceSearchDto();
        }
        PageResponse<FlowInstance> selectPage = flowInstanceMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowInstancePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowInstancePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowInstance> buildQueryWrapper(FlowInstanceSearchDto searchDto) {
        LambdaQueryWrapperX<FlowInstance> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowInstance::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowInstance getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowInstance data = flowInstanceMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
