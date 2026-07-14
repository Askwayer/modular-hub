package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowHisTaskCreateDto;
import com.vita.workflow.flow.dto.FlowHisTaskSearchDto;
import com.vita.workflow.flow.dto.FlowHisTaskUpdateDto;
import com.vita.workflow.flow.entity.FlowHisTask;
import com.vita.workflow.flow.mapper.FlowHisTaskMapper;
import com.vita.workflow.flow.service.IFlowHisTaskService;
import com.vita.workflow.flow.vo.FlowHisTaskDetailVo;
import com.vita.workflow.flow.vo.FlowHisTaskListVo;
import com.vita.workflow.flow.vo.FlowHisTaskPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 历史任务记录表 Service 实现
 * @version 1.0
 */
@Service
public class FlowHisTaskServiceImpl extends ServiceImpl<FlowHisTaskMapper, FlowHisTask> implements IFlowHisTaskService {

    @Resource
    private FlowHisTaskMapper flowHisTaskMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowHisTaskCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowHisTask data = new FlowHisTask();
        BeanUtils.copyProperties(createDto, data);
        flowHisTaskMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowHisTaskUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowHisTask data = new FlowHisTask();
        BeanUtils.copyProperties(updateDto, data);
        flowHisTaskMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowHisTaskMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowHisTaskDetailVo get(Long id) {
        FlowHisTask data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowHisTaskDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowHisTaskListVo> list(FlowHisTaskSearchDto searchDto) {
        List<FlowHisTask> dataList = flowHisTaskMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowHisTaskListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowHisTaskPageVo> page(FlowHisTaskSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowHisTaskSearchDto();
        }
        PageResponse<FlowHisTask> selectPage = flowHisTaskMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowHisTaskPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowHisTaskPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowHisTask> buildQueryWrapper(FlowHisTaskSearchDto searchDto) {
        LambdaQueryWrapperX<FlowHisTask> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowHisTask::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowHisTask getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowHisTask data = flowHisTaskMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
