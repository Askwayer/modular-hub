package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.dto.AiMessageSendDto;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.enums.AiChatRequestModeEnum;
import com.vita.aichat.enums.AiConversationStatusEnum;
import com.vita.aichat.enums.AiMessageTypeEnum;
import com.vita.aichat.enums.AiRoleTypeEnum;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI Chat 请求准备器。
 *
 * <p>负责请求大模型前的所有准备工作，包括：前置校验、锁获取、消息构建、Prompt 组装等。</p>
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
     * 准备发送消息上下文。
     *
     * <p>执行完整的请求前准备流程：前置校验 → 锁获取 → 数据准备 → 上下文组装。</p>
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @param requestMode    请求模式
     * @return 发送准备上下文
     */
    public PreparedSendContext prepareSendContext(Long conversationId,
                                                  AiMessageSendDto sendDto,
                                                  AiChatRequestModeEnum requestMode) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        String requestId = generateRequestId(sendDto.getRequestId());
        validateSendDto(sendDto);
        AiConversation conversation = requireConversation(conversationId);
        validateConversationStatus(conversation);
        validateRequestUniqueness(conversationId, requestId);
        acquireConversationLock(conversationId, requestId);

        try {
            return doPrepareSendContext(conversation, userId, requestId, sendDto, requestMode);
        } catch (RuntimeException ex) {
            aiChatRedisCoordinator.releaseConversationLock(conversationId);
            aiChatRedisCoordinator.releaseRequestMark(conversationId, requestId);
            throw ex;
        }
    }

    /**
     * 校验会话存在性。
     *
     * @param conversationId 会话ID
     * @return 会话实体
     */
    public AiConversation requireConversation(Long conversationId) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
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
     * 获取会话锁。
     *
     * @param conversationId 会话ID
     * @param requestId      请求唯一标识
     */
    private void acquireConversationLock(Long conversationId, String requestId) {
        if (!aiChatRedisCoordinator.acquireConversationLock(conversationId, requestId)) {
            aiChatRedisCoordinator.releaseRequestMark(conversationId, requestId);
            throw new ServiceException(GlobalErrorCode.CHAT_GENERATING_CONFLICT);
        }
    }

    /**
     * 执行发送上下文准备。
     *
     * @param conversation 会话实体
     * @param userId       用户ID
     * @param requestId    请求唯一标识
     * @param sendDto      消息发送请求对象
     * @param requestMode  请求模式
     * @return 发送准备上下文
     */
    @Transactional(rollbackFor = Exception.class)
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
        AiChatRequest chatRequest = aiChatMessageBuilder.buildChatRequest(
                conversation.getId(), userId, requestId, requestMode, modelCode,
                userMessage.getId(), assistantMessage.getId());
        try {
            aiChatRequestService.create(chatRequest);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(GlobalErrorCode.CHAT_REQUEST_DUPLICATE);
        }

        // 构建 Prompt
        Prompt prompt = buildPrompt(conversation, userId, userMessage, modelCode,
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
        context.setPrompt(prompt);
        return context;
    }

    /**
     * 构建 AI 提示词。
     *
     * @param conversation 会话实体
     * @param userId       用户ID
     * @param userMessage  用户消息实体
     * @param modelCode    模型编码
     * @param stream       是否流式
     * @return 提示词对象
     */
    private Prompt buildPrompt(AiConversation conversation,
                               Long userId,
                               AiMessage userMessage,
                               String modelCode,
                               boolean stream) {
        List<Message> messages = new ArrayList<>();

        // 添加系统提示词
        String systemPrompt = generateSystemPrompt(conversation.getSystemPrompt());
        if (CharSequenceUtil.isNotBlank(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 添加会话历史摘要
        AiConversationSummary summary = aiConversationSummaryService.getLatestReadySummary(
                conversation.getId(), userId);
        if (summary != null && CharSequenceUtil.isNotBlank(summary.getSummaryText())) {
            messages.add(new SystemMessage("以下是当前会话的历史摘要：\n" + summary.getSummaryText()));
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
            Message promptMessage = convertToPromptMessage(historyMessage);
            if (promptMessage != null) {
                messages.add(promptMessage);
            }
        }

        // 添加当前用户消息
        messages.add(new UserMessage(userMessage.getContent()));

        return new Prompt(messages, buildChatOptions(conversation, modelCode, stream));
    }

    /**
     * 将消息实体转换为提示词消息。
     *
     * @param aiMessage 消息实体
     * @return 提示词消息
     */
    private Message convertToPromptMessage(AiMessage aiMessage) {
        if (aiMessage == null || CharSequenceUtil.isBlank(aiMessage.getContent())) {
            return null;
        }
        return switch (AiRoleTypeEnum.valueOf(aiMessage.getRoleType())) {
            case USER -> new UserMessage(aiMessage.getContent());
            case ASSISTANT -> new AssistantMessage(aiMessage.getContent());
            case SYSTEM -> new SystemMessage(aiMessage.getContent());
            case SUMMARY -> new SystemMessage("历史摘要：\n" + aiMessage.getContent());
        };
    }

    /**
     * 构建模型调用选项。
     *
     * @param conversation 会话实体
     * @param modelCode    模型编码
     * @param stream       是否流式
     * @return 模型选项
     */
    private com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions buildChatOptions(
            AiConversation conversation, String modelCode, boolean stream) {
        var builder = com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions.builder()
                .model(modelCode)
                .stream(stream)
                .incrementalOutput(stream)
                .enableSearch(Boolean.TRUE)
                .enableThinking(isThinkingModel(modelCode));

        if (conversation.getMaxTokens() != null) {
            builder.maxToken(conversation.getMaxTokens());
        }
        if (conversation.getTemperature() != null) {
            builder.temperature(conversation.getTemperature().doubleValue());
        }
        return builder.build();
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
                ? aiChatProperty.getDashscopeChatModel()
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
