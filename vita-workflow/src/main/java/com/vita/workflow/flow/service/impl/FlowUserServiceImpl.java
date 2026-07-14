package com.vita.workflow.flow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.flow.dto.FlowUserCreateDto;
import com.vita.workflow.flow.dto.FlowUserSearchDto;
import com.vita.workflow.flow.dto.FlowUserUpdateDto;
import com.vita.workflow.flow.entity.FlowUser;
import com.vita.workflow.flow.mapper.FlowUserMapper;
import com.vita.workflow.flow.service.IFlowUserService;
import com.vita.workflow.flow.vo.FlowUserDetailVo;
import com.vita.workflow.flow.vo.FlowUserListVo;
import com.vita.workflow.flow.vo.FlowUserPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.workflow.flow.service.impl
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程用户表 Service 实现
 * @version 1.0
 */
@Service
public class FlowUserServiceImpl extends ServiceImpl<FlowUserMapper, FlowUser> implements IFlowUserService {

    @Resource
    private FlowUserMapper flowUserMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(FlowUserCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowUser data = new FlowUser();
        BeanUtils.copyProperties(createDto, data);
        flowUserMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(FlowUserUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        FlowUser data = new FlowUser();
        BeanUtils.copyProperties(updateDto, data);
        flowUserMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        flowUserMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public FlowUserDetailVo get(Long id) {
        FlowUser data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, FlowUserDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<FlowUserListVo> list(FlowUserSearchDto searchDto) {
        List<FlowUser> dataList = flowUserMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, FlowUserListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<FlowUserPageVo> page(FlowUserSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new FlowUserSearchDto();
        }
        PageResponse<FlowUser> selectPage = flowUserMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<FlowUserPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), FlowUserPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<FlowUser> buildQueryWrapper(FlowUserSearchDto searchDto) {
        LambdaQueryWrapperX<FlowUser> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(FlowUser::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private FlowUser getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        FlowUser data = flowUserMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
