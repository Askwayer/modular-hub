package com.vita.aichat.aiConversation.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.aichat.aiConversation.dto.AiConversationCreateDto;
import com.vita.aichat.aiConversation.dto.AiConversationSearchDto;
import com.vita.aichat.aiConversation.dto.AiConversationUpdateDto;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.mapper.AiConversationMapper;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.aichat.aiConversation.vo.AiConversationDetailVo;
import com.vita.aichat.aiConversation.vo.AiConversationListVo;
import com.vita.aichat.aiConversation.vo.AiConversationPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.service.impl
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 Service 实现
 * @version 1.0
 */
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation> implements IAiConversationService {

    @Resource
    private AiConversationMapper aiConversationMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(AiConversationCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiConversation data = new AiConversation();
        BeanUtils.copyProperties(createDto, data);
        aiConversationMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(AiConversationUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        AiConversation data = new AiConversation();
        BeanUtils.copyProperties(updateDto, data);
        aiConversationMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        aiConversationMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public AiConversationDetailVo get(Long id) {
        AiConversation data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, AiConversationDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<AiConversationListVo> list(AiConversationSearchDto searchDto) {
        List<AiConversation> dataList = aiConversationMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, AiConversationListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<AiConversationPageVo> page(AiConversationSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new AiConversationSearchDto();
        }
        PageResponse<AiConversation> selectPage = aiConversationMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<AiConversationPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), AiConversationPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<AiConversation> buildQueryWrapper(AiConversationSearchDto searchDto) {
        LambdaQueryWrapperX<AiConversation> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(AiConversation::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private AiConversation getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiConversation data = aiConversationMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
