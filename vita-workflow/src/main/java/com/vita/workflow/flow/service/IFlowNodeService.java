package com.vita.workflow.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowNodeCreateDto;
import com.vita.workflow.flow.dto.FlowNodeSearchDto;
import com.vita.workflow.flow.dto.FlowNodeUpdateDto;
import com.vita.workflow.flow.entity.FlowNode;
import com.vita.workflow.flow.vo.FlowNodeDetailVo;
import com.vita.workflow.flow.vo.FlowNodeListVo;
import com.vita.workflow.flow.vo.FlowNodePageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程节点表 Service 接口
 * @version 1.0
 */
public interface IFlowNodeService extends IService<FlowNode> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(FlowNodeCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(FlowNodeUpdateDto updateDto);

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
    FlowNodeDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<FlowNodeListVo> list(FlowNodeSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<FlowNodePageVo> page(FlowNodeSearchDto searchDto);
}
