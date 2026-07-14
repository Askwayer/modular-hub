package com.vita.workflow.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowFormCreateDto;
import com.vita.workflow.flow.dto.FlowFormSearchDto;
import com.vita.workflow.flow.dto.FlowFormUpdateDto;
import com.vita.workflow.flow.entity.FlowForm;
import com.vita.workflow.flow.vo.FlowFormDetailVo;
import com.vita.workflow.flow.vo.FlowFormListVo;
import com.vita.workflow.flow.vo.FlowFormPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程表单表 Service 接口
 * @version 1.0
 */
public interface IFlowFormService extends IService<FlowForm> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(FlowFormCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(FlowFormUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    FlowFormDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<FlowFormListVo> list(FlowFormSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<FlowFormPageVo> page(FlowFormSearchDto searchDto);
}
