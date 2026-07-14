package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowFormCreateDto;
import com.vita.workflow.flow.dto.FlowFormSearchDto;
import com.vita.workflow.flow.dto.FlowFormUpdateDto;
import com.vita.workflow.flow.entity.FlowForm;
import com.vita.workflow.flow.mapper.FlowFormMapper;
import com.vita.workflow.flow.service.IFlowFormService;
import com.vita.workflow.flow.vo.FlowFormDetailVo;
import com.vita.workflow.flow.vo.FlowFormListVo;
import com.vita.workflow.flow.vo.FlowFormPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程表单表 Service 实现
 * @version 1.0
 */
@Service
public class FlowFormServiceImpl extends ServiceImpl<FlowFormMapper, FlowForm> implements IFlowFormService {

    @Resource
    private FlowFormMapper flowFormMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowFormCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowForm data = new FlowForm();
        BeanUtils.copyProperties(createDto, data);
        flowFormMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowFormUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowForm data = new FlowForm();
        BeanUtils.copyProperties(updateDto, data);
        flowFormMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowFormMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowFormDetailVo get(Long id) {
        FlowForm data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowFormDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowFormListVo> list(FlowFormSearchDto searchDto) {
        List<FlowForm> dataList = flowFormMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowFormListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowFormPageVo> page(FlowFormSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowFormSearchDto();
        }
        PageResponse<FlowForm> selectPage = flowFormMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowFormPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowFormPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowForm> buildQueryWrapper(FlowFormSearchDto searchDto) {
        LambdaQueryWrapperX<FlowForm> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowForm::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowForm getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowForm data = flowFormMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
