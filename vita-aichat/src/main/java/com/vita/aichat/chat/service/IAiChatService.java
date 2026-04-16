package com.vita.aichat.chat.service;

import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.vo.AiChatSendVo;
import com.vita.aichat.chat.vo.AiConversationDetailVo;
import com.vita.aichat.chat.vo.AiConversationPageVo;
import com.vita.aichat.chat.vo.AiMessagePageVo;
import com.vita.core.page.PageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Chat 聚合服务。
 */
public interface IAiChatService {

    /**
     * 创建会话。
     *
     * @param createDto 创建请求对象
     * @return 会话ID
     */
    Long createConversation(AiConversationCreateDto createDto);

    /**
     * 分页查询会话列表。
     *
     * @param queryDto 分页查询条件
     * @return 会话分页数据
     */
    PageResponse<AiConversationPageVo> pageConversations(AiConversationPageQueryDto queryDto);

    /**
     * 查询会话详情。
     *
     * @param conversationId 会话ID
     * @return 会话详情对象
     */
    AiConversationDetailVo getConversation(Long conversationId);

    /**
     * 重命名会话标题。
     *
     * @param conversationId 会话ID
     * @param updateDto 标题更新请求对象
     */
    void renameConversation(Long conversationId, AiConversationTitleUpdateDto updateDto);

    /**
     * 删除会话及其关联数据。
     *
     * @param conversationId 会话ID
     */
    void deleteConversation(Long conversationId);

    /**
     * 分页查询消息列表。
     *
     * @param conversationId 会话ID
     * @param queryDto 分页查询条件
     * @return 消息分页数据
     */
    PageResponse<AiMessagePageVo> pageMessages(Long conversationId, AiMessagePageQueryDto queryDto);

    /**
     * 发送消息（同步模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto 消息发送请求对象
     * @return 发送结果
     */
    AiChatSendVo sendMessage(Long conversationId, AiMessageSendDto sendDto);

    /**
     * 发送消息（流式模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto 消息发送请求对象
     * @return SSE流式响应
     */
    SseEmitter sendMessageStream(Long conversationId, AiMessageSendDto sendDto);
}
