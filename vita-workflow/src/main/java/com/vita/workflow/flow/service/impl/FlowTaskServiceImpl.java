package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowTaskCreateDto;
import com.vita.workflow.flow.dto.FlowTaskSearchDto;
import com.vita.workflow.flow.dto.FlowTaskUpdateDto;
import com.vita.workflow.flow.entity.FlowTask;
import com.vita.workflow.flow.mapper.FlowTaskMapper;
import com.vita.workflow.flow.service.IFlowTaskService;
import com.vita.workflow.flow.vo.FlowTaskDetailVo;
import com.vita.workflow.flow.vo.FlowTaskListVo;
import com.vita.workflow.flow.vo.FlowTaskPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 待办任务表 Service 实现
 * @version 1.0
 */
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskMapper, FlowTask> implements IFlowTaskService {

    @Resource
    private FlowTaskMapper flowTaskMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowTaskCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowTask data = new FlowTask();
        BeanUtils.copyProperties(createDto, data);
        flowTaskMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowTaskUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowTask data = new FlowTask();
        BeanUtils.copyProperties(updateDto, data);
        flowTaskMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowTaskMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowTaskDetailVo get(Long id) {
        FlowTask data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowTaskDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowTaskListVo> list(FlowTaskSearchDto searchDto) {
        List<FlowTask> dataList = flowTaskMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowTaskListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowTaskPageVo> page(FlowTaskSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowTaskSearchDto();
        }
        PageResponse<FlowTask> selectPage = flowTaskMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowTaskPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowTaskPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowTask> buildQueryWrapper(FlowTaskSearchDto searchDto) {
        LambdaQueryWrapperX<FlowTask> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowTask::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowTask getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowTask data = flowTaskMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
