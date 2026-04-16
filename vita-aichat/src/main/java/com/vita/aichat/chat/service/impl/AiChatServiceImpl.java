package com.vita.aichat.chat.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatProperties;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.AiChatConstants;
import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.support.AiChatAssembler;
import com.vita.aichat.chat.support.AiChatStreamWriter;
import com.vita.aichat.chat.support.SseEmitterAiChatStreamWriter;
import com.vita.aichat.chat.vo.AiChatConversationDetailVo;
import com.vita.aichat.chat.vo.AiChatConversationPageVo;
import com.vita.aichat.chat.vo.AiChatMessageVo;
import com.vita.aichat.chat.vo.AiChatSendRespVo;
import com.vita.aichat.config.AiChatProperties;
import com.vita.aichat.enums.*;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.utils.SnowUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * 编排正式 Chat 的会话管理、消息落库、Prompt 组装和模型调用流程。
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private final IAiConversationService aiConversationService;
    private final IAiMessageService aiMessageService;
    private final IAiConversationSummaryService aiConversationSummaryService;
    private final ChatModel chatModel;
    private final Executor aiChatExecutor;
    private final AiChatProperties aiChatProperties;
    private final DashScopeChatProperties dashScopeChatProperties;

    public AiChatServiceImpl(IAiConversationService aiConversationService,
                             IAiMessageService aiMessageService,
                             IAiConversationSummaryService aiConversationSummaryService,
                             ChatModel chatModel,
                             @Qualifier("aiChatExecutor") Executor aiChatExecutor,
                             AiChatProperties aiChatProperties,
                             DashScopeChatProperties dashScopeChatProperties) {
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.chatModel = chatModel;
        this.aiChatExecutor = aiChatExecutor;
        this.aiChatProperties = aiChatProperties;
        this.dashScopeChatProperties = dashScopeChatProperties;
    }

    /**
     * 为当前登录用户创建一条可立即对话的会话记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(AiChatConversationCreateReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = new AiConversation();
        conversation.setId(SnowUtils.getSnowflakeNextId());
        conversation.setUserId(userId);
        conversation.setTitle(CharSequenceUtil.blankToDefault(reqDto.getTitle(), AiChatConstants.DEFAULT_TITLE));
        conversation.setTitleSource(AiTitleSourceEnum.AUTO.name());
        conversation.setStatus(AiConversationStatusEnum.ACTIVE.name());
        conversation.setPinned((byte) 0);
        conversation.setMessageCount(0);
        conversation.setRoundCount(0);
        conversation.setCurrentModelCode(defaultModelCode(reqDto.getModelCode()));
        conversation.setSystemPrompt(defaultSystemPrompt(reqDto.getSystemPrompt()));
        conversation.setTemperature(reqDto.getTemperature());
        conversation.setMaxTokens(reqDto.getMaxTokens());
        conversation.setMaxContextMessages(defaultMaxContextMessages(reqDto.getMaxContextMessages()));
        conversation.setSummaryVersion(0);
        conversation.setSummaryStatus(AiSummaryStatusEnum.NONE.name());
        aiConversationService.create(conversation);
        return conversation.getId();
    }

    @Override
    public PageResponse<AiChatConversationPageVo> pageConversations(AiChatConversationPageReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PageResponse<AiConversation> page = aiConversationService.pageByUserId(reqDto, userId, reqDto.getKeyword());
        List<AiChatConversationPageVo> list = new ArrayList<>();
        for (AiConversation item : page.getList()) {
            list.add(AiChatAssembler.toConversationPageVo(item));
        }
        return new PageResponse<>(list, page.getTotal());
    }

    @Override
    public AiChatConversationDetailVo getConversation(Long conversationId) {
        return AiChatAssembler.toConversationDetailVo(
                requireConversation(conversationId, LoginUserInfoModelContext.getRequiredLoginUserId())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameConversation(Long conversationId, AiChatConversationRenameReqDto reqDto) {
        AiConversation conversation = requireConversation(conversationId, LoginUserInfoModelContext.getRequiredLoginUserId());
        conversation.setTitle(reqDto.getTitle().trim());
        conversation.setTitleSource(AiTitleSourceEnum.MANUAL.name());
        aiConversationService.update(conversation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = requireConversation(conversationId, userId);
        aiConversationService.deleteById(conversation.getId());
        aiMessageService.deleteByConversationIdAndUserId(conversationId, userId);
        aiConversationSummaryService.deleteByConversationIdAndUserId(conversationId, userId);
    }

    @Override
    public PageResponse<AiChatMessageVo> pageMessages(Long conversationId, AiChatMessagePageReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        requireConversation(conversationId, userId);
        PageResponse<AiMessage> page = aiMessageService.pageByConversationIdAndUserId(reqDto, conversationId, userId);
        List<AiChatMessageVo> list = new ArrayList<>();
        for (AiMessage item : page.getList()) {
            list.add(AiChatAssembler.toMessageVo(item));
        }
        return new PageResponse<>(list, page.getTotal());
    }

    /**
     * 写入用户消息、助手占位消息，再同步调用 DashScope 模型返回完整回复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSendRespVo sendMessage(Long conversationId, AiChatMessageSendReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PreparedSendContext context = prepareSend(conversationId, reqDto, userId);
        try {
            AiChatCompletionResult completionResult = callModel(context);
            handleGatewaySuccess(context, completionResult);
            return AiChatAssembler.toSendResp(context, completionResult);
        } catch (ModelCallException ex) {
            handleGatewayFailure(context, ex.getErrorCode(), ex.getServiceException().getMessage());
            throw ex.getServiceException();
        }
    }

    /**
     * 写入会话消息后启动异步模型调用，并通过 SSE 推送增量回复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter streamMessage(Long conversationId, AiChatMessageSendReqDto reqDto) {
        if (!Boolean.TRUE.equals(aiChatProperties.getStreamEnabled())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST).setMessage("当前环境未开启流式回复");
        }
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PreparedSendContext context = prepareSend(conversationId, reqDto, userId);
        SseEmitter emitter = new SseEmitter(aiChatProperties.getReadTimeoutMs());
        AiChatStreamWriter writer = new SseEmitterAiChatStreamWriter(emitter);
        aiChatExecutor.execute(() -> doStreamMessage(context, writer));
        return emitter;
    }

    /**
     * 执行真实的流式模型调用，并把生命周期事件写回消息表和 SSE。
     */
    void doStreamMessage(PreparedSendContext context, AiChatStreamWriter writer) {
        String[] actualModelCode = {defaultModelCode(context.getConversation().getCurrentModelCode())};
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        TokenUsageAccumulator usageAccumulator = new TokenUsageAccumulator();
        writer.sendStart(context.getAssistantMessage().getId(), context.getRequestId(), actualModelCode[0]);
        try {
            Prompt prompt = buildPrompt(context, actualModelCode[0], true);
            chatModel.stream(prompt)
                    .doOnNext(response -> {
                        AiChatCompletionResult chunk = toCompletionResult(response, actualModelCode[0]);
                        actualModelCode[0] = chunk.getModelCode();
                        if (CharSequenceUtil.isNotBlank(chunk.getContent())) {
                            contentBuilder.append(chunk.getContent());
                            writer.sendContentDelta(chunk.getContent());
                        }
                        if (CharSequenceUtil.isNotBlank(chunk.getReasoningContent())) {
                            reasoningBuilder.append(chunk.getReasoningContent());
                            writer.sendReasoningDelta(chunk.getReasoningContent());
                        }
                        usageAccumulator.update(chunk);
                    })
                    .blockLast();

            AiChatCompletionResult result = buildFinalCompletionResult(
                    actualModelCode[0],
                    contentBuilder.toString(),
                    reasoningBuilder.toString(),
                    usageAccumulator
            );
            handleGatewaySuccess(context, result);
            writer.sendEnd(result);
            writer.complete();
        } catch (Exception ex) {
            ModelCallException mapped = mapModelException(ex);
            handleGatewayFailure(context, mapped.getErrorCode(), mapped.getServiceException().getMessage());
            writer.sendError(mapped.getErrorCode(), mapped.getServiceException().getMessage());
            writer.completeWithError(mapped.getServiceException());
        }
    }

    /**
     * 校验会话归属并落库用户消息、助手占位消息，为后续模型调用建立上下文。
     */
    PreparedSendContext prepareSend(Long conversationId, AiChatMessageSendReqDto reqDto, Long userId) {
        validateMessageType(reqDto.getMessageType());
        AiConversation conversation = requireConversation(conversationId, userId);
        if (!AiConversationStatusEnum.ACTIVE.name().equals(conversation.getStatus())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST).setMessage("当前会话不可发送消息");
        }
        AiMessage userMessage = buildUserMessage(conversation, reqDto, userId);
        aiMessageService.create(userMessage);
        AiMessage assistantMessage = buildAssistantPlaceholder(conversation, userMessage, userId);
        aiMessageService.create(assistantMessage);

        PreparedSendContext context = new PreparedSendContext();
        context.setConversation(conversation);
        context.setUserMessage(userMessage);
        context.setAssistantMessage(assistantMessage);
        context.setUserId(userId);
        context.setRequestId(userMessage.getRequestId());
        return context;
    }

    private AiConversation requireConversation(Long conversationId, Long userId) {
        if (conversationId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiConversation conversation = aiConversationService.getByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND).setMessage("会话不存在");
        }
        return conversation;
    }

    private AiMessage buildUserMessage(AiConversation conversation, AiChatMessageSendReqDto reqDto, Long userId) {
        AiMessage message = new AiMessage();
        message.setId(SnowUtils.getSnowflakeNextId());
        message.setConversationId(conversation.getId());
        message.setUserId(userId);
        message.setRoleType(AiRoleTypeEnum.USER.name());
        message.setMessageType(defaultMessageType(reqDto.getMessageType()));
        message.setRequestId(defaultRequestId(reqDto.getRequestId()));
        message.setModelCode(defaultModelCode(conversation.getCurrentModelCode()));
        message.setContent(reqDto.getContent().trim());
        message.setContentStatus(AiContentStatusEnum.DONE.name());
        message.setUseContext(defaultUseContext(reqDto.getUseContext()));
        return message;
    }

    private AiMessage buildAssistantPlaceholder(AiConversation conversation, AiMessage userMessage, Long userId) {
        AiMessage message = new AiMessage();
        message.setId(SnowUtils.getSnowflakeNextId());
        message.setConversationId(conversation.getId());
        message.setUserId(userId);
        message.setRoleType(AiRoleTypeEnum.ASSISTANT.name());
        message.setMessageType(AiChatConstants.MESSAGE_TYPE_TEXT);
        message.setReplyToId(userMessage.getId());
        message.setRequestId(userMessage.getRequestId());
        message.setModelCode(defaultModelCode(conversation.getCurrentModelCode()));
        message.setContentStatus(AiContentStatusEnum.GENERATING.name());
        message.setUseContext((byte) 1);
        message.setContent("");
        return message;
    }

    /**
     * 读取摘要和上下文消息，组装成最终的 Spring AI Prompt。
     */
    Prompt buildPrompt(PreparedSendContext context, String modelCode, boolean stream) {
        AiConversation conversation = context.getConversation();
        AiConversationSummary summary = aiConversationSummaryService
                .getLatestReadySummary(conversation.getId(), context.getUserId());
        List<AiMessage> history = aiMessageService.listRecentContextMessages(
                conversation.getId(),
                context.getUserId(),
                context.getUserMessage().getId(),
                defaultMaxContextMessages(conversation.getMaxContextMessages())
        );
        List<Message> messages = new ArrayList<>();
        String systemPrompt = defaultSystemPrompt(conversation.getSystemPrompt());
        if (CharSequenceUtil.isNotBlank(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.addAll(AiChatAssembler.buildPromptMessages(summary, history, context.getUserMessage()));
        return new Prompt(messages, buildOptions(conversation, modelCode, stream));
    }

    DashScopeChatOptions buildOptions(AiConversation conversation, String modelCode, boolean stream) {
        DashScopeChatOptions.DashScopeChatOptionsBuilder builder = DashScopeChatOptions.builder()
                .model(modelCode)
                .stream(stream);
        if (conversation.getTemperature() != null) {
            builder.temperature(conversation.getTemperature().doubleValue());
        }
        if (conversation.getMaxTokens() != null) {
            builder.maxToken(conversation.getMaxTokens());
        }
        if (stream) {
            builder.incrementalOutput(Boolean.TRUE);
        }
        if (isThinkingModel(modelCode)) {
            builder.enableThinking(Boolean.TRUE);
        }
        return builder.build();
    }

    /**
     * 模型回复成功后更新助手消息状态，并回写会话统计。
     */
    private void handleGatewaySuccess(PreparedSendContext context, AiChatCompletionResult response) {
        AiMessage assistantMessage = context.getAssistantMessage();
        assistantMessage.setContent(defaultText(response.getContent()));
        assistantMessage.setReasoningContent(response.getReasoningContent());
        assistantMessage.setModelCode(defaultModelCode(response.getModelCode()));
        assistantMessage.setContentStatus(AiContentStatusEnum.DONE.name());
        assistantMessage.setTokenInput(response.getTokenInput());
        assistantMessage.setTokenOutput(response.getTokenOutput());
        assistantMessage.setTokenTotal(response.getTokenTotal());
        assistantMessage.setErrorCode(null);
        assistantMessage.setErrorMessage(null);
        aiMessageService.update(assistantMessage);
        updateConversationAfterReply(context.getConversation(), assistantMessage);
    }

    /**
     * 模型回复失败时标记助手消息错误状态，并保留错误码和错误摘要。
     */
    private void handleGatewayFailure(PreparedSendContext context, String errorCode, String errorMessage) {
        AiMessage assistantMessage = context.getAssistantMessage();
        assistantMessage.setContentStatus(AiContentStatusEnum.ERROR.name());
        assistantMessage.setErrorCode(errorCode);
        assistantMessage.setErrorMessage(normalizeErrorMessage(errorMessage));
        aiMessageService.update(assistantMessage);
        updateConversationAfterReply(context.getConversation(), assistantMessage);
    }

    private void updateConversationAfterReply(AiConversation conversation, AiMessage assistantMessage) {
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setLastMessageAt(LocalDateTime.now());
        update.setMessageCount((conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + 2);
        update.setRoundCount((conversation.getRoundCount() == null ? 0 : conversation.getRoundCount()) + 1);
        update.setCurrentModelCode(defaultModelCode(assistantMessage.getModelCode()));
        aiConversationService.update(update);
        conversation.setMessageCount(update.getMessageCount());
        conversation.setRoundCount(update.getRoundCount());
        conversation.setLastMessageAt(update.getLastMessageAt());
        conversation.setCurrentModelCode(update.getCurrentModelCode());
    }

    AiChatCompletionResult callModel(PreparedSendContext context) {
        String modelCode = defaultModelCode(context.getConversation().getCurrentModelCode());
        Prompt prompt = buildPrompt(context, modelCode, false);
        try {
            ChatResponse response = chatModel.call(prompt);
            return toCompletionResult(response, modelCode);
        } catch (ModelCallException ex) {
            throw ex;
        } catch (Exception ex) {
            throw mapModelException(ex);
        }
    }

    AiChatCompletionResult toCompletionResult(ChatResponse response, String fallbackModelCode) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new ModelCallException(
                    AiChatConstants.MODEL_RESPONSE_INVALID,
                    new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE).setMessage("模型返回内容为空")
            );
        }

        AiChatCompletionResult result = new AiChatCompletionResult();
        result.setModelCode(extractModelCode(response, fallbackModelCode));
        result.setContent(defaultText(response.getResult().getOutput().getText()));
        result.setReasoningContent(extractReasoningContent(response));
        fillUsage(response, result);
        return result;
    }

    private String extractModelCode(ChatResponse response, String fallbackModelCode) {
        ChatResponseMetadata metadata = response.getMetadata();
        if (CharSequenceUtil.isNotBlank(metadata.getModel())) {
            return metadata.getModel();
        }
        return fallbackModelCode;
    }

    private String extractReasoningContent(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return "";
        }
        ChatGenerationMetadata metadata = response.getResult().getMetadata();
        String reasoning = extractMetadataValue(metadata, "reasoningContent", "reasoning_content");
        if (CharSequenceUtil.isNotBlank(reasoning)) {
            return reasoning;
        }
        Map<String, Object> outputMetadata = response.getResult().getOutput().getMetadata();
        return extractMetadataValue(outputMetadata, "reasoningContent", "reasoning_content");
    }

    private String extractMetadataValue(ChatGenerationMetadata metadata, String... keys) {
        if (metadata == null) {
            return "";
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && CharSequenceUtil.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private String extractMetadataValue(Map<String, Object> metadata, String... keys) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && CharSequenceUtil.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private void fillUsage(ChatResponse response, AiChatCompletionResult result) {
        ChatResponseMetadata metadata = response.getMetadata();
        if (metadata == null) {
            return;
        }
        Usage usage = metadata.getUsage();
        if (usage == null) {
            return;
        }
        result.setTokenInput(usage.getPromptTokens());
        result.setTokenOutput(usage.getCompletionTokens());
        result.setTokenTotal(usage.getTotalTokens());
    }

    private AiChatCompletionResult buildFinalCompletionResult(String modelCode,
                                                              String content,
                                                              String reasoningContent,
                                                              TokenUsageAccumulator usageAccumulator) {
        AiChatCompletionResult result = new AiChatCompletionResult();
        result.setModelCode(modelCode);
        result.setContent(content);
        result.setReasoningContent(reasoningContent);
        result.setTokenInput(usageAccumulator.tokenInput);
        result.setTokenOutput(usageAccumulator.tokenOutput);
        result.setTokenTotal(usageAccumulator.tokenTotal);
        return result;
    }

    ModelCallException mapModelException(Throwable throwable) {
        Throwable root = rootCause(throwable);
        String message = normalizeErrorMessage(root.getMessage());
        String lowerMessage = message.toLowerCase(Locale.ROOT);

        if (isUnauthorized(root, lowerMessage)) {
            return new ModelCallException(
                    AiChatConstants.MODEL_AUTH_ERROR,
                    new ServiceException(GlobalErrorCode.FORBIDDEN).setMessage("模型鉴权失败，请检查 DashScope API Key 配置")
            );
        }
        if (root instanceof SocketTimeoutException || root instanceof TimeoutException || lowerMessage.contains("timeout")) {
            return new ModelCallException(
                    AiChatConstants.MODEL_TIMEOUT,
                    new ServiceException(GlobalErrorCode.GATEWAY_TIMEOUT).setMessage("模型响应超时，请稍后重试")
            );
        }
        if (hasStatus(root, 429) || lowerMessage.contains("rate limit") || lowerMessage.contains("too many requests")) {
            return new ModelCallException(
                    AiChatConstants.MODEL_RATE_LIMIT,
                    new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS).setMessage("模型请求过于频繁，请稍后重试")
            );
        }
        if (root instanceof ConnectException || lowerMessage.contains("connection refused") || lowerMessage.contains("network")) {
            return new ModelCallException(
                    AiChatConstants.MODEL_NETWORK_ERROR,
                    new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE).setMessage("模型网络不可用，请检查 DashScope 地址配置")
            );
        }
        return new ModelCallException(
                AiChatConstants.MODEL_PROVIDER_ERROR,
                new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE).setMessage("模型调用失败，请检查 DashScope 服务状态")
        );
    }

    private boolean isUnauthorized(Throwable throwable, String message) {
        return hasStatus(throwable, 401)
                || hasStatus(throwable, 403)
                || message.contains("401")
                || message.contains("403")
                || message.contains("unauthorized")
                || message.contains("forbidden");
    }

    private boolean hasStatus(Throwable throwable, int statusCode) {
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().value() == statusCode;
        }
        if (throwable instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value() == statusCode;
        }
        return false;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private boolean isThinkingModel(String modelCode) {
        return CharSequenceUtil.isNotBlank(modelCode)
                && modelCode.trim().toLowerCase(Locale.ROOT).startsWith("qwq");
    }

    private void validateMessageType(String messageType) {
        String actualMessageType = defaultMessageType(messageType);
        if (!AiChatConstants.MESSAGE_TYPE_TEXT.equals(actualMessageType)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST).setMessage("当前仅支持文本消息");
        }
    }

    private String defaultModelCode(String modelCode) {
        String configuredModel = dashScopeChatProperties.getOptions() != null
                ? dashScopeChatProperties.getOptions().getModel()
                : null;
        return CharSequenceUtil.blankToDefault(modelCode, configuredModel);
    }

    private String defaultSystemPrompt(String systemPrompt) {
        return CharSequenceUtil.blankToDefault(systemPrompt, aiChatProperties.getDefaultSystemPrompt());
    }

    private Integer defaultMaxContextMessages(Integer maxContextMessages) {
        return maxContextMessages != null ? maxContextMessages : aiChatProperties.getMaxContextMessages();
    }

    private Byte defaultUseContext(Byte useContext) {
        return useContext != null ? useContext : (byte) 1;
    }

    private String defaultRequestId(String requestId) {
        return CharSequenceUtil.blankToDefault(requestId, "chat-" + UUID.randomUUID().toString().replace("-", ""));
    }

    private String defaultMessageType(String messageType) {
        return CharSequenceUtil.blankToDefault(messageType, AiChatConstants.MESSAGE_TYPE_TEXT);
    }

    private String normalizeErrorMessage(String errorMessage) {
        return CharSequenceUtil.blankToDefault(errorMessage, "模型调用失败");
    }

    private String defaultText(String value) {
        return value != null ? value : "";
    }

    static final class ModelCallException extends RuntimeException {

        private final String errorCode;

        private final ServiceException serviceException;

        ModelCallException(String errorCode, ServiceException serviceException) {
            super(serviceException.getMessage(), serviceException);
            this.errorCode = errorCode;
            this.serviceException = serviceException;
        }

        String getErrorCode() {
            return errorCode;
        }

        ServiceException getServiceException() {
            return serviceException;
        }
    }

    static final class TokenUsageAccumulator {

        private Integer tokenInput;

        private Integer tokenOutput;

        private Integer tokenTotal;

        void update(AiChatCompletionResult result) {
            if (result.getTokenInput() != null) {
                tokenInput = result.getTokenInput();
            }
            if (result.getTokenOutput() != null) {
                tokenOutput = result.getTokenOutput();
            }
            if (result.getTokenTotal() != null) {
                tokenTotal = result.getTokenTotal();
            }
        }
    }
}
