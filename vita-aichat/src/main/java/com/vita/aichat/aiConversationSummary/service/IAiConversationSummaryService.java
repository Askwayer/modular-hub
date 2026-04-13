package com.vita.aichat.aiConversationSummary.service;

import com.vita.core.page.PageResponse;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummaryCreateDto;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummarySearchDto;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummaryUpdateDto;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryDetailVo;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryListVo;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversationSummary.service
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 Service 接口
 * @version 1.0
 */
public interface IAiConversationSummaryService extends IService<AiConversationSummary> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(AiConversationSummaryCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(AiConversationSummaryUpdateDto updateDto);

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
    AiConversationSummaryDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<AiConversationSummaryListVo> list(AiConversationSummarySearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<AiConversationSummaryPageVo> page(AiConversationSummarySearchDto searchDto);
}
