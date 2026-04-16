package com.vita.aichat.chat.support;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.chat.dto.AiMessageSendDto;
import com.vita.aichat.enums.*;
import com.vita.utils.SnowUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI Chat 消息构建器。
 *
 * <p>负责构建用户消息、助手消息占位符和聊天请求实体。</p>
 */
@Component
public class AiChatMessageBuilder {

    /**
     * 构建用户消息实体。
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param requestId      请求唯一标识
     * @param modelCode      模型编码
     * @param sendDto        消息发送请求对象
     * @return 用户消息实体
     */
    public AiMessage buildUserMessage(Long conversationId,
                                      Long userId,
                                      String requestId,
                                      String modelCode,
                                      AiMessageSendDto sendDto) {
        AiMessage userMessage = new AiMessage();
        userMessage.setId(SnowUtils.getSnowflakeNextId());
        userMessage.setConversationId(conversationId);
        userMessage.setUserId(userId);
        userMessage.setRoleType(AiRoleTypeEnum.USER.name());
        userMessage.setMessageType(defaultMessageType(sendDto.getMessageType()));
        userMessage.setRequestId(requestId);
        userMessage.setModelCode(modelCode);
        userMessage.setContent(sendDto.getContent().trim());
        userMessage.setContentStatus(AiContentStatusEnum.DONE.name());
        userMessage.setUseContext(sendDto.getUseContext() == null ? (byte) 1 : sendDto.getUseContext());
        userMessage.setStreamBatchCount(0);
        return userMessage;
    }

    /**
     * 构建助手消息占位符。
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param requestId      请求唯一标识
     * @param modelCode      模型编码
     * @param replyToId      回复消息ID
     * @return 助手消息实体
     */
    public AiMessage buildAssistantPlaceholder(Long conversationId,
                                               Long userId,
                                               String requestId,
                                               String modelCode,
                                               Long replyToId) {
        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setId(SnowUtils.getSnowflakeNextId());
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setUserId(userId);
        assistantMessage.setRoleType(AiRoleTypeEnum.ASSISTANT.name());
        assistantMessage.setMessageType(AiMessageTypeEnum.TEXT.name());
        assistantMessage.setReplyToId(replyToId);
        assistantMessage.setRequestId(requestId);
        assistantMessage.setModelCode(modelCode);
        assistantMessage.setContent("");
        assistantMessage.setReasoningContent("");
        assistantMessage.setContentStatus(AiContentStatusEnum.GENERATING.name());
        assistantMessage.setUseContext((byte) 1);
        assistantMessage.setStreamBatchCount(0);
        return assistantMessage;
    }

    /**
     * 构建聊天请求实体。
     *
     * @param conversationId     会话ID
     * @param userId             用户ID
     * @param requestId          请求唯一标识
     * @param requestMode        请求模式
     * @param modelCode          模型编码
     * @param userMessageId      用户消息ID
     * @param assistantMessageId 助手消息ID
     * @return 聊天请求实体
     */
    public AiChatRequest buildChatRequest(Long conversationId,
                                          Long userId,
                                          String requestId,
                                          AiChatRequestModeEnum requestMode,
                                          String modelCode,
                                          Long userMessageId,
                                          Long assistantMessageId) {
        AiChatRequest chatRequest = new AiChatRequest();
        chatRequest.setId(SnowUtils.getSnowflakeNextId());
        chatRequest.setConversationId(conversationId);
        chatRequest.setUserId(userId);
        chatRequest.setRequestId(requestId);
        chatRequest.setRequestMode(requestMode.name());
        chatRequest.setStatus(AiChatRequestStatusEnum.PENDING.name());
        chatRequest.setUserMessageId(userMessageId);
        chatRequest.setAssistantMessageId(assistantMessageId);
        chatRequest.setModelCode(modelCode);
        chatRequest.setStartedAt(LocalDateTime.now());
        return chatRequest;
    }

    /**
     * 获取默认消息类型。
     *
     * @param messageType 消息类型
     * @return 默认消息类型
     */
    private String defaultMessageType(String messageType) {
        return cn.hutool.core.text.CharSequenceUtil.isBlank(messageType)
                ? AiMessageTypeEnum.TEXT.name()
                : messageType.trim().toUpperCase();
    }
}
