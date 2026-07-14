package com.vita.workflow.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowDefinitionCreateDto;
import com.vita.workflow.flow.dto.FlowDefinitionSearchDto;
import com.vita.workflow.flow.dto.FlowDefinitionUpdateDto;
import com.vita.workflow.flow.entity.FlowDefinition;
import com.vita.workflow.flow.vo.FlowDefinitionDetailVo;
import com.vita.workflow.flow.vo.FlowDefinitionListVo;
import com.vita.workflow.flow.vo.FlowDefinitionPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程定义表 Service 接口
 * @version 1.0
 */
public interface IFlowDefinitionService extends IService<FlowDefinition> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(FlowDefinitionCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(FlowDefinitionUpdateDto updateDto);

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
    FlowDefinitionDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<FlowDefinitionListVo> list(FlowDefinitionSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<FlowDefinitionPageVo> page(FlowDefinitionSearchDto searchDto);
}
