package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowNodeCreateDto;
import com.vita.workflow.flow.dto.FlowNodeSearchDto;
import com.vita.workflow.flow.dto.FlowNodeUpdateDto;
import com.vita.workflow.flow.entity.FlowNode;
import com.vita.workflow.flow.mapper.FlowNodeMapper;
import com.vita.workflow.flow.service.IFlowNodeService;
import com.vita.workflow.flow.vo.FlowNodeDetailVo;
import com.vita.workflow.flow.vo.FlowNodeListVo;
import com.vita.workflow.flow.vo.FlowNodePageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程节点表 Service 实现
 * @version 1.0
 */
@Service
public class FlowNodeServiceImpl extends ServiceImpl<FlowNodeMapper, FlowNode> implements IFlowNodeService {

    @Resource
    private FlowNodeMapper flowNodeMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowNodeCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowNode data = new FlowNode();
        BeanUtils.copyProperties(createDto, data);
        flowNodeMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowNodeUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowNode data = new FlowNode();
        BeanUtils.copyProperties(updateDto, data);
        flowNodeMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowNodeMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowNodeDetailVo get(Long id) {
        FlowNode data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowNodeDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowNodeListVo> list(FlowNodeSearchDto searchDto) {
        List<FlowNode> dataList = flowNodeMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowNodeListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowNodePageVo> page(FlowNodeSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowNodeSearchDto();
        }
        PageResponse<FlowNode> selectPage = flowNodeMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowNodePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowNodePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowNode> buildQueryWrapper(FlowNodeSearchDto searchDto) {
        LambdaQueryWrapperX<FlowNode> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowNode::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowNode getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowNode data = flowNodeMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
