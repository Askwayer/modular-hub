package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.ai.core.model.AiChatMessage;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.dto.AiMessageSendDto;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.context.ReservedSendContext;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.enums.AiChatRequestModeEnum;
import com.vita.aichat.enums.AiConversationStatusEnum;
import com.vita.aichat.enums.AiMessageTypeEnum;
import com.vita.aichat.enums.AiRoleTypeEnum;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI Chat 请求准备器。
 *
 * <p>负责请求大模型前的所有准备工作，包括：前置校验、requestId 预留、消息构建、Prompt 组装等。</p>
 */
@Component
public class AiChatRequestPreparer {

    private final IAiConversationService aiConversationService;
    private final IAiMessageService aiMessageService;
    private final IAiConversationSummaryService aiConversationSummaryService;
    private final IAiChatRequestService aiChatRequestService;
    private final AiChatProperty aiChatProperty;
    private final AiChatRedisCoordinator aiChatRedisCoordinator;
    private final AiChatMessageBuilder aiChatMessageBuilder;

    public AiChatRequestPreparer(IAiConversationService aiConversationService,
                                 IAiMessageService aiMessageService,
                                 IAiConversationSummaryService aiConversationSummaryService,
                                 IAiChatRequestService aiChatRequestService,
                                 AiChatProperty aiChatProperty,
                                 AiChatRedisCoordinator aiChatRedisCoordinator,
                                 AiChatMessageBuilder aiChatMessageBuilder) {
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiChatProperty = aiChatProperty;
        this.aiChatRedisCoordinator = aiChatRedisCoordinator;
        this.aiChatMessageBuilder = aiChatMessageBuilder;
    }

    /**
     * 预留一次发送请求。
     *
     * <p>执行轻量前置校验：参数校验 → 会话状态检查 → requestId 防重预留。</p>
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @return 预留上下文
     */
    public ReservedSendContext reserveSendContext(Long conversationId, AiMessageSendDto sendDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        String requestId = generateRequestId(sendDto.getRequestId());
        validateSendDto(sendDto);
        AiConversation conversation = requireConversation(conversationId);
        validateConversationStatus(conversation);
        validateRequestUniqueness(conversationId, requestId);
        ReservedSendContext context = new ReservedSendContext();
        context.setUserId(userId);
        context.setConversationId(conversationId);
        context.setRequestId(requestId);
        context.setConversation(conversation);
        context.setSendDto(sendDto);
        return context;
    }

    /**
     * 校验会话存在性。
     *
     * @param conversationId 会话ID
     * @return 会话实体
     */
    public AiConversation requireConversation(Long conversationId) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        return requireConversation(conversationId, userId);
    }

    /**
     * 按指定用户校验会话存在性。
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @return 会话实体
     */
    public AiConversation requireConversation(Long conversationId, Long userId) {
        AiConversation conversation = aiConversationService.getByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new ServiceException(GlobalErrorCode.CHAT_CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    /**
     * 校验消息发送请求参数。
     *
     * @param sendDto 消息发送请求对象
     */
    private void validateSendDto(AiMessageSendDto sendDto) {
        String messageType = generateMessageType(sendDto.getMessageType());
        if (!AiMessageTypeEnum.TEXT.name().equals(messageType)) {
            throw new ServiceException(GlobalErrorCode.CHAT_MESSAGE_TYPE_UNSUPPORTED);
        }
    }

    /**
     * 校验会话状态是否允许发送消息。
     *
     * @param conversation 会话实体
     */
    private void validateConversationStatus(AiConversation conversation) {
        if (!AiConversationStatusEnum.ACTIVE.name().equals(conversation.getStatus())) {
            throw new ServiceException(GlobalErrorCode.CHAT_CONVERSATION_NOT_ACTIVE);
        }
    }

    /**
     * 校验请求唯一性。
     *
     * @param conversationId 会话ID
     * @param requestId      请求唯一标识
     */
    private void validateRequestUniqueness(Long conversationId, String requestId) {
        if (aiChatRequestService.getByConversationIdAndRequestId(conversationId, requestId) != null
                || !aiChatRedisCoordinator.markRequestIfAbsent(conversationId, requestId)) {
            throw new ServiceException(GlobalErrorCode.CHAT_REQUEST_DUPLICATE);
        }
    }

    /**
     * 执行发送上下文准备。
     *
     * <p>调用前必须确保已经拿到全局活跃名额和会话锁。</p>
     *
     * @param reservedContext 预留上下文
     * @param requestMode     请求模式
     * @return 发送准备上下文
     */
    public PreparedSendContext prepareSendContext(ReservedSendContext reservedContext,
                                                  AiChatRequestModeEnum requestMode) {
        AiConversation conversation = requireConversation(reservedContext.getConversationId(), reservedContext.getUserId());
        validateConversationStatus(conversation);
        try {
            return doPrepareSendContext(
                    conversation,
                    reservedContext.getUserId(),
                    reservedContext.getRequestId(),
                    reservedContext.getSendDto(),
                    requestMode
            );
        } catch (RuntimeException ex) {
            aiChatRedisCoordinator.releaseRequestMark(reservedContext.getConversationId(), reservedContext.getRequestId());
            throw ex;
        }
    }

    /**
     * 释放请求预留标记。
     *
     * @param reservedContext 预留上下文
     */
    public void releaseRequestReservation(ReservedSendContext reservedContext) {
        if (reservedContext != null) {
            aiChatRedisCoordinator.releaseRequestMark(reservedContext.getConversationId(), reservedContext.getRequestId());
        }
    }

    /**
     * 事务内完成消息、请求和 Prompt 组装。
     *
     * @param conversation 会话实体
     * @param userId       用户ID
     * @param requestId    请求唯一标识
     * @param sendDto      消息发送请求对象
     * @param requestMode  请求模式
     * @return 发送准备上下文
     */
    public PreparedSendContext doPrepareSendContext(AiConversation conversation,
                                                    Long userId,
                                                    String requestId,
                                                    AiMessageSendDto sendDto,
                                                    AiChatRequestModeEnum requestMode) {
        String modelCode = generateModelCode(conversation.getCurrentModelCode());

        // 构建并持久化消息实体
        AiMessage userMessage = aiChatMessageBuilder.buildUserMessage(
                conversation.getId(), userId, requestId, modelCode, sendDto);
        aiMessageService.create(userMessage);

        AiMessage assistantMessage = aiChatMessageBuilder.buildAssistantPlaceholder(
                conversation.getId(), userId, requestId, modelCode, userMessage.getId());
        aiMessageService.create(assistantMessage);

        // 构建并持久化聊天请求
        com.vita.aichat.aiChatRequest.entity.AiChatRequest chatRequest = aiChatMessageBuilder.buildChatRequest(
                conversation.getId(), userId, requestId, requestMode, modelCode,
                userMessage.getId(), assistantMessage.getId());
        try {
            aiChatRequestService.create(chatRequest);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(GlobalErrorCode.CHAT_REQUEST_DUPLICATE);
        }

        // 构建统一 AI 请求 DTO
        com.vita.ai.core.model.AiChatRequest aiChatRequest = buildAiChatRequest(
                conversation, userId, userMessage, modelCode, sendDto,
                requestMode == AiChatRequestModeEnum.STREAM);

        // 更新会话状态
        updateConversationForPreparedRequest(conversation, requestId, modelCode);

        // 组装上下文
        PreparedSendContext context = new PreparedSendContext();
        context.setUserId(userId);
        context.setConversationId(conversation.getId());
        context.setRequestId(requestId);
        context.setModelCode(modelCode);
        context.setConversation(conversation);
        context.setUserMessage(userMessage);
        context.setAssistantMessage(assistantMessage);
        context.setChatRequest(chatRequest);
        context.setAiChatRequest(aiChatRequest);
        return context;
    }

    /**
     * 构建统一 AI 请求 DTO。
     */
    private com.vita.ai.core.model.AiChatRequest buildAiChatRequest(AiConversation conversation,
                                                                      Long userId,
                                                                      AiMessage userMessage,
                                                                      String modelCode,
                                                                      AiMessageSendDto sendDto,
                                                                      boolean stream) {
        List<AiChatMessage> messages = new ArrayList<>();

        // 添加会话历史摘要
        AiConversationSummary summary = aiConversationSummaryService.getLatestReadySummary(
                conversation.getId(), userId);
        if (summary != null && CharSequenceUtil.isNotBlank(summary.getSummaryText())) {
            messages.add(AiChatMessage.builder()
                    .role("system")
                    .content("以下是当前会话的历史摘要：\n" + summary.getSummaryText())
                    .build());
        }

        // 添加历史消息
        List<AiMessage> historyMessages = aiMessageService.listRecentContextMessages(
                conversation.getId(),
                userId,
                userMessage.getId(),
                generateMaxContextMessages(conversation.getMaxContextMessages())
        );
        Collections.reverse(historyMessages);
        for (AiMessage historyMessage : historyMessages) {
            AiChatMessage chatMessage = convertToAiChatMessage(historyMessage);
            if (chatMessage != null) {
                messages.add(chatMessage);
            }
        }

        String systemPrompt = generateSystemPrompt(conversation.getSystemPrompt());
        return com.vita.ai.core.model.AiChatRequest.builder()
                .requestId(userMessage.getRequestId())
                .module("aichat")
                .conversationId(conversation.getId())
                .userId(userId)
                .provider(null)
                .model(modelCode)
                .systemPrompt(CharSequenceUtil.isNotBlank(systemPrompt) ? systemPrompt : null)
                .userPrompt(userMessage.getContent())
                .messages(messages)
                .temperature(conversation.getTemperature())
                .maxTokens(conversation.getMaxTokens())
                .stream(stream)
                .build();
    }

    /**
     * 将消息实体转换为统一 AiChatMessage。
     */
    private AiChatMessage convertToAiChatMessage(AiMessage aiMessage) {
        if (aiMessage == null || CharSequenceUtil.isBlank(aiMessage.getContent())) {
            return null;
        }
        String role = switch (AiRoleTypeEnum.valueOf(aiMessage.getRoleType())) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM, SUMMARY -> "system";
        };
        String content = AiRoleTypeEnum.valueOf(aiMessage.getRoleType()) == AiRoleTypeEnum.SUMMARY
                ? "历史摘要：\n" + aiMessage.getContent()
                : aiMessage.getContent();
        return AiChatMessage.builder().role(role).content(content).build();
    }

    /**
     * 更新会话预准备请求信息。
     *
     * @param conversation 会话实体
     * @param requestId    请求唯一标识
     * @param modelCode    模型编码
     */
    private void updateConversationForPreparedRequest(AiConversation conversation,
                                                      String requestId,
                                                      String modelCode) {
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setActiveRequestId(requestId);
        update.setCurrentModelCode(modelCode);
        update.setLastMessageAt(LocalDateTime.now());
        update.setMessageCount(generateNumber(conversation.getMessageCount()) + 2);
        update.setRoundCount(generateNumber(conversation.getRoundCount()) + 1);
        update.setLastErrorCode(null);
        update.setLastErrorMessage(null);
        aiConversationService.update(update);

        // 同步更新内存中的会话对象
        conversation.setMessageCount(update.getMessageCount());
        conversation.setRoundCount(update.getRoundCount());
        conversation.setCurrentModelCode(modelCode);
        conversation.setActiveRequestId(requestId);
    }

    /**
     * 判断是否为思考模型。
     *
     * @param modelCode 模型编码
     * @return 是否为思考模型
     */
    private boolean isThinkingModel(String modelCode) {
        return CharSequenceUtil.isNotBlank(modelCode) && modelCode.startsWith("qwq-");
    }

    /**
     * 生成默认会话标题。
     *
     * @param title 标题
     * @return 默认标题
     */
    public String generateConversationTitle(String title) {
        return CharSequenceUtil.isBlank(title) ? "新对话" : title.trim();
    }

    /**
     * 生成默认系统提示词。
     *
     * @param systemPrompt 系统提示词
     * @return 默认系统提示词
     */
    public String generateSystemPrompt(String systemPrompt) {
        return CharSequenceUtil.isBlank(systemPrompt)
                ? aiChatProperty.getDefaultSystemPrompt()
                : systemPrompt.trim();
    }

    /**
     * 生成默认模型编码。
     *
     * @param modelCode 模型编码
     * @return 默认模型编码
     */
    public String generateModelCode(String modelCode) {
        return CharSequenceUtil.isBlank(modelCode)
                ? "qwen-plus"
                : modelCode.trim();
    }

    /**
     * 生成默认最大上下文消息数。
     *
     * @param maxContextMessages 最大上下文消息数
     * @return 默认最大上下文消息数
     */
    public Integer generateMaxContextMessages(Integer maxContextMessages) {
        return maxContextMessages == null || maxContextMessages <= 0
                ? aiChatProperty.getMaxContextMessages()
                : maxContextMessages;
    }

    /**
     * 生成默认请求唯一标识。
     *
     * @param requestId 请求唯一标识
     * @return 默认请求唯一标识
     */
    private String generateRequestId(String requestId) {
        return CharSequenceUtil.isBlank(requestId) ? IdUtil.fastSimpleUUID() : requestId.trim();
    }

    /**
     * 生成默认消息类型。
     *
     * @param messageType 消息类型
     * @return 默认消息类型
     */
    private String generateMessageType(String messageType) {
        return CharSequenceUtil.isBlank(messageType)
                ? AiMessageTypeEnum.TEXT.name()
                : messageType.trim().toUpperCase();
    }

    /**
     * 生成默认数值。
     *
     * @param value 数值
     * @return 默认数值
     */
    private int generateNumber(Integer value) {
        return value == null ? 0 : value;
    }
}
