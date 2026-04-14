package com.vita.aichat.chat.service;

import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.vo.AiChatConversationDetailVo;
import com.vita.aichat.chat.vo.AiChatConversationPageVo;
import com.vita.aichat.chat.vo.AiChatMessageVo;
import com.vita.aichat.chat.vo.AiChatSendRespVo;
import com.vita.core.page.PageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 正式 Chat 应用服务，只暴露任务导向接口。
 */
public interface IAiChatService {

    /**
     * 创建当前登录用户的会话。
     */
    Long createConversation(AiChatConversationCreateReqDto reqDto);

    /**
     * 分页查询当前用户的会话列表。
     */
    PageResponse<AiChatConversationPageVo> pageConversations(AiChatConversationPageReqDto reqDto);

    /**
     * 读取当前用户的会话详情。
     */
    AiChatConversationDetailVo getConversation(Long conversationId);

    /**
     * 手动重命名会话标题。
     */
    void renameConversation(Long conversationId, AiChatConversationRenameReqDto reqDto);

    /**
     * 逻辑删除会话及其关联消息、摘要。
     */
    void deleteConversation(Long conversationId);

    /**
     * 分页查询会话历史消息。
     */
    PageResponse<AiChatMessageVo> pageMessages(Long conversationId, AiChatMessagePageReqDto reqDto);

    /**
     * 同步发送消息并等待完整回复。
     */
    AiChatSendRespVo sendMessage(Long conversationId, AiChatMessageSendReqDto reqDto);

    /**
     * 发送消息并以 SSE 输出流式回复。
     */
    SseEmitter streamMessage(Long conversationId, AiChatMessageSendReqDto reqDto);
}
