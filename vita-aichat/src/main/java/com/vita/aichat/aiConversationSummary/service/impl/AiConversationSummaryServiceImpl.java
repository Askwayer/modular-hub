package com.vita.aichat.aiConversationSummary.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummaryCreateDto;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummarySearchDto;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummaryUpdateDto;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.mapper.AiConversationSummaryMapper;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryDetailVo;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryListVo;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.service.impl
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 Service 实现
 * @version 1.0
 */
@Service
public class AiConversationSummaryServiceImpl extends ServiceImpl<AiConversationSummaryMapper, AiConversationSummary> implements IAiConversationSummaryService {

    @Resource
    private AiConversationSummaryMapper aiConversationSummaryMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(AiConversationSummaryCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiConversationSummary data = new AiConversationSummary();
        BeanUtils.copyProperties(createDto, data);
        aiConversationSummaryMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(AiConversationSummaryUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        AiConversationSummary data = new AiConversationSummary();
        BeanUtils.copyProperties(updateDto, data);
        aiConversationSummaryMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        aiConversationSummaryMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public AiConversationSummaryDetailVo get(Long id) {
        AiConversationSummary data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, AiConversationSummaryDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<AiConversationSummaryListVo> list(AiConversationSummarySearchDto searchDto) {
        List<AiConversationSummary> dataList = aiConversationSummaryMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, AiConversationSummaryListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<AiConversationSummaryPageVo> page(AiConversationSummarySearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new AiConversationSummarySearchDto();
        }
        PageResponse<AiConversationSummary> selectPage = aiConversationSummaryMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<AiConversationSummaryPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), AiConversationSummaryPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<AiConversationSummary> buildQueryWrapper(AiConversationSummarySearchDto searchDto) {
        LambdaQueryWrapperX<AiConversationSummary> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(AiConversationSummary::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private AiConversationSummary getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiConversationSummary data = aiConversationSummaryMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
