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

    Long createConversation(AiConversationCreateDto createDto);

    PageResponse<AiConversationPageVo> pageConversations(AiConversationPageQueryDto queryDto);

    AiConversationDetailVo getConversation(Long conversationId);

    void renameConversation(Long conversationId, AiConversationTitleUpdateDto updateDto);

    void deleteConversation(Long conversationId);

    PageResponse<AiMessagePageVo> pageMessages(Long conversationId, AiMessagePageQueryDto queryDto);

    AiChatSendVo sendMessage(Long conversationId, AiMessageSendDto sendDto);

    SseEmitter sendMessageStream(Long conversationId, AiMessageSendDto sendDto);
}
