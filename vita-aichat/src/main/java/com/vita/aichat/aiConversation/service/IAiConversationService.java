package com.vita.aichat.aiConversation.service;

import com.vita.core.page.PageResponse;
import com.vita.aichat.aiConversation.dto.AiConversationCreateDto;
import com.vita.aichat.aiConversation.dto.AiConversationSearchDto;
import com.vita.aichat.aiConversation.dto.AiConversationUpdateDto;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.aichat.aiConversation.vo.AiConversationDetailVo;
import com.vita.aichat.aiConversation.vo.AiConversationListVo;
import com.vita.aichat.aiConversation.vo.AiConversationPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.service
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 Service 接口
 * @version 1.0
 */
public interface IAiConversationService extends IService<AiConversation> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(AiConversationCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(AiConversationUpdateDto updateDto);

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
    AiConversationDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<AiConversationListVo> list(AiConversationSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<AiConversationPageVo> page(AiConversationSearchDto searchDto);
}
