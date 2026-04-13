package com.vita.aichat.aiMessage.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.LambdaQueryWrapperX;
import com.vita.aichat.aiMessage.dto.AiMessageCreateDto;
import com.vita.aichat.aiMessage.dto.AiMessageSearchDto;
import com.vita.aichat.aiMessage.dto.AiMessageUpdateDto;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.mapper.AiMessageMapper;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.aichat.aiMessage.vo.AiMessageDetailVo;
import com.vita.aichat.aiMessage.vo.AiMessageListVo;
import com.vita.aichat.aiMessage.vo.AiMessagePageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiMessage.service.impl
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 Service 实现
 * @version 1.0
 */
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements IAiMessageService {

    @Resource
    private AiMessageMapper aiMessageMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(AiMessageCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiMessage data = new AiMessage();
        BeanUtils.copyProperties(createDto, data);
        aiMessageMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(AiMessageUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        AiMessage data = new AiMessage();
        BeanUtils.copyProperties(updateDto, data);
        aiMessageMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        aiMessageMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public AiMessageDetailVo get(Long id) {
        AiMessage data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, AiMessageDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<AiMessageListVo> list(AiMessageSearchDto searchDto) {
        List<AiMessage> dataList = aiMessageMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, AiMessageListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<AiMessagePageVo> page(AiMessageSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new AiMessageSearchDto();
        }
        PageResponse<AiMessage> selectPage = aiMessageMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<AiMessagePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), AiMessagePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<AiMessage> buildQueryWrapper(AiMessageSearchDto searchDto) {
        LambdaQueryWrapperX<AiMessage> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(AiMessage::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private AiMessage getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiMessage data = aiMessageMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
