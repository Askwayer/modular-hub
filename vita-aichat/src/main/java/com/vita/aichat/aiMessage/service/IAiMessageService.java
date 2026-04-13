package com.vita.aichat.aiMessage.service;

import com.vita.core.page.PageResponse;
import com.vita.aichat.aiMessage.dto.AiMessageCreateDto;
import com.vita.aichat.aiMessage.dto.AiMessageSearchDto;
import com.vita.aichat.aiMessage.dto.AiMessageUpdateDto;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.aichat.aiMessage.vo.AiMessageDetailVo;
import com.vita.aichat.aiMessage.vo.AiMessageListVo;
import com.vita.aichat.aiMessage.vo.AiMessagePageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiMessage.service
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 Service 接口
 * @version 1.0
 */
public interface IAiMessageService extends IService<AiMessage> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(AiMessageCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(AiMessageUpdateDto updateDto);

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
    AiMessageDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<AiMessageListVo> list(AiMessageSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<AiMessagePageVo> page(AiMessageSearchDto searchDto);
}
