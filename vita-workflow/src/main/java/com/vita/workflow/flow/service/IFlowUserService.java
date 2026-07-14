package com.vita.workflow.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowUserCreateDto;
import com.vita.workflow.flow.dto.FlowUserSearchDto;
import com.vita.workflow.flow.dto.FlowUserUpdateDto;
import com.vita.workflow.flow.entity.FlowUser;
import com.vita.workflow.flow.vo.FlowUserDetailVo;
import com.vita.workflow.flow.vo.FlowUserListVo;
import com.vita.workflow.flow.vo.FlowUserPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程用户表 Service 接口
 * @version 1.0
 */
public interface IFlowUserService extends IService<FlowUser> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(FlowUserCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(FlowUserUpdateDto updateDto);

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
    FlowUserDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<FlowUserListVo> list(FlowUserSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<FlowUserPageVo> page(FlowUserSearchDto searchDto);
}
