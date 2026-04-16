package com.vita.aichat.chat.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.aiMessageStreamBatch.entity.AiMessageStreamBatch;
import com.vita.aichat.aiMessageStreamBatch.service.IAiMessageStreamBatchService;
import com.vita.aichat.chat.config.AiChatProperties;
import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.model.result.AiChatResponseChunk;
import com.vita.aichat.chat.model.result.AiChatStreamBatchSnapshot;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.support.*;
import com.vita.aichat.chat.vo.*;
import com.vita.aichat.enums.*;
import com.vita.aichat.error.AiChatErrorCode;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.ErrorCode;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.utils.SnowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * AI Chat 聚合服务实现。
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private static final long STREAM_BATCH_FLUSH_MILLIS = 1000L;

    private static final int STREAM_BATCH_CHAR_THRESHOLD = 512;

    private final IAiConversationService aiConversationService;
    private final IAiMessageService aiMessageService;
    private final IAiConversationSummaryService aiConversationSummaryService;
    private final IAiChatRequestService aiChatRequestService;
    private final IAiMessageStreamBatchService aiMessageStreamBatchService;
    private final ChatModel chatModel;
    private final AiChatProperties aiChatProperties;
    private final AiChatAssembler aiChatAssembler;
    private final AiChatResponseExtractor aiChatResponseExtractor;
    private final AiChatRedisCoordinator aiChatRedisCoordinator;
    private final AiChatSseWriter aiChatSseWriter;
    private final Executor aiChatExecutor;
    private final TransactionTemplate transactionTemplate;

    public AiChatServiceImpl(IAiConversationService aiConversationService,
                             IAiMessageService aiMessageService,
                             IAiConversationSummaryService aiConversationSummaryService,
                             IAiChatRequestService aiChatRequestService,
                             IAiMessageStreamBatchService aiMessageStreamBatchService,
                             ChatModel chatModel,
                             AiChatProperties aiChatProperties,
                             AiChatAssembler aiChatAssembler,
                             AiChatResponseExtractor aiChatResponseExtractor,
                             AiChatRedisCoordinator aiChatRedisCoordinator,
                             AiChatSseWriter aiChatSseWriter,
                             @Qualifier("aiChatExecutor") Executor aiChatExecutor,
                             TransactionTemplate transactionTemplate) {
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiMessageStreamBatchService = aiMessageStreamBatchService;
        this.chatModel = chatModel;
        this.aiChatProperties = aiChatProperties;
        this.aiChatAssembler = aiChatAssembler;
        this.aiChatResponseExtractor = aiChatResponseExtractor;
        this.aiChatRedisCoordinator = aiChatRedisCoordinator;
        this.aiChatSseWriter = aiChatSseWriter;
        this.aiChatExecutor = aiChatExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Long createConversation(AiConversationCreateDto createDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = new AiConversation();
        conversation.setId(SnowUtils.getSnowflakeNextId());
        conversation.setUserId(userId);
        conversation.setTitle(defaultConversationTitle(createDto.getTitle()));
        conversation.setTitleSource(AiTitleSourceEnum.AUTO.name());
        conversation.setStatus(AiConversationStatusEnum.ACTIVE.name());
        conversation.setPinned((byte) 0);
        conversation.setMessageCount(0);
        conversation.setRoundCount(0);
        conversation.setSummaryVersion(0);
        conversation.setSummaryStatus(AiSummaryStatusEnum.NONE.name());
        conversation.setCurrentModelCode(defaultModelCode(createDto.getModelCode()));
        conversation.setSystemPrompt(defaultSystemPrompt(createDto.getSystemPrompt()));
        conversation.setTemperature(createDto.getTemperature());
        conversation.setMaxTokens(createDto.getMaxTokens());
        conversation.setMaxContextMessages(defaultMaxContextMessages(createDto.getMaxContextMessages()));
        aiConversationService.create(conversation);
        return conversation.getId();
    }

    @Override
    public PageResponse<AiConversationPageVo> pageConversations(AiConversationPageQueryDto queryDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PageResponse<AiConversation> pageResponse = aiConversationService.pageByUserId(queryDto, userId, queryDto.getKeyword());
        PageResponse<AiConversationPageVo> response = new PageResponse<>();
        response.setTotal(pageResponse.getTotal());
        response.setList(pageResponse.getList().stream().map(aiChatAssembler::toConversationPageVo).toList());
        return response;
    }

    @Override
    public AiConversationDetailVo getConversation(Long conversationId) {
        return aiChatAssembler.toConversationDetailVo(requireConversation(conversationId));
    }

    @Override
    public void renameConversation(Long conversationId, AiConversationTitleUpdateDto updateDto) {
        AiConversation conversation = requireConversation(conversationId);
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setTitle(updateDto.getTitle().trim());
        update.setTitleSource(AiTitleSourceEnum.MANUAL.name());
        aiConversationService.update(update);
    }

    @Override
    public void deleteConversation(Long conversationId) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = requireConversation(conversationId);
        List<AiMessage> messages = aiMessageService.listByConversationIdAndUserId(conversation.getId(), userId);
        List<Long> assistantMessageIds = messages.stream()
                .map(AiMessage::getId)
                .filter(Objects::nonNull)
                .toList();
        transactionTemplate.executeWithoutResult(status -> {
            aiMessageStreamBatchService.deleteByAssistantMessageIds(assistantMessageIds);
            aiChatRequestService.deleteByConversationIdAndUserId(conversation.getId(), userId);
            aiConversationSummaryService.deleteByConversationIdAndUserId(conversation.getId(), userId);
            aiMessageService.deleteByConversationIdAndUserId(conversation.getId(), userId);
            aiConversationService.deleteByIdAndUserId(conversation.getId(), userId);
        });
    }

    @Override
    public PageResponse<AiMessagePageVo> pageMessages(Long conversationId, AiMessagePageQueryDto queryDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        requireConversation(conversationId);
        PageResponse<AiMessage> pageResponse = aiMessageService.pageByConversationIdAndUserId(queryDto, conversationId, userId);
        PageResponse<AiMessagePageVo> response = new PageResponse<>();
        response.setTotal(pageResponse.getTotal());
        response.setList(pageResponse.getList().stream().map(aiChatAssembler::toMessagePageVo).toList());
        return response;
    }

    @Override
    public AiChatSendVo sendMessage(Long conversationId, AiMessageSendDto sendDto) {
        PreparedSendContext context = prepareSendContext(conversationId, sendDto, AiChatRequestModeEnum.SYNC);
        try {
            ChatResponse response = chatModel.call(context.getPrompt());
            AiChatCompletionResult completionResult = aiChatResponseExtractor.extractCompletion(response);
            validateCompletion(completionResult);
            LocalDateTime completedAt = LocalDateTime.now();
            completionResult.setFirstTokenAt(completionResult.getFirstTokenAt() != null ? completionResult.getFirstTokenAt() : completedAt);
            completionResult.setCompletedAt(completedAt);
            completionResult.setStreamBatchCount(0);
            applySuccessOutcome(context, completionResult, AiChatRequestStatusEnum.SUCCESS.name());
            return aiChatAssembler.toSendVo(context.getUserMessage(), context.getAssistantMessage(), completionResult);
        } catch (Exception ex) {
            ServiceException serviceException = convertToServiceException(ex);
            applyFailureOutcome(context, emptyCompletionResult(), serviceException, AiChatRequestStatusEnum.ERROR.name());
            throw serviceException;
        } finally {
            aiChatRedisCoordinator.releaseConversationLock(conversationId);
        }
    }

    @Override
    public SseEmitter sendMessageStream(Long conversationId, AiMessageSendDto sendDto) {
        if (!Boolean.TRUE.equals(aiChatProperties.getStreamEnabled())) {
            throw new ServiceException(AiChatErrorCode.CHAT_STREAM_DISABLED);
        }

        PreparedSendContext context = prepareSendContext(conversationId, sendDto, AiChatRequestModeEnum.STREAM);
        SseEmitter emitter = aiChatSseWriter.createEmitter();

        AiChatStreamEventVo startEvent = aiChatAssembler.toStreamEventVo(
                context.getConversationId(),
                context.getAssistantMessage().getId(),
                context.getRequestId(),
                0
        );
        startEvent.setContentStatus(AiContentStatusEnum.GENERATING.name());
        sendStreamSuccessQuietly(emitter, "message-start", context.getRequestId(), startEvent);

        try {
            aiChatExecutor.execute(() -> processStream(context, emitter));
        } catch (RejectedExecutionException ex) {
            ServiceException serviceException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE);
            applyFailureOutcome(context, emptyCompletionResult(), serviceException, AiChatRequestStatusEnum.ERROR.name());
            sendStreamErrorQuietly(emitter, context.getRequestId(), serviceException.getCode(), serviceException.getMessage(), startEvent);
            emitter.complete();
            aiChatRedisCoordinator.releaseConversationLock(conversationId);
        }
        return emitter;
    }

    private void processStream(PreparedSendContext context, SseEmitter emitter) {
        AiStreamBatchAccumulator batchAccumulator = new AiStreamBatchAccumulator(
                STREAM_BATCH_FLUSH_MILLIS,
                STREAM_BATCH_CHAR_THRESHOLD,
                System.currentTimeMillis()
        );
        StringBuilder fullContent = new StringBuilder();
        StringBuilder fullReasoning = new StringBuilder();
        LocalDateTime firstTokenAt = null;
        String providerRequestId = null;
        String finishReason = null;
        Integer tokenInput = null;
        Integer tokenOutput = null;
        Integer tokenTotal = null;
        int providerSequence = 0;
        int batchNo = 0;

        markRequestStatus(context, AiChatRequestStatusEnum.STREAMING.name());

        try {
            Iterable<ChatResponse> responses = chatModel.stream(context.getPrompt()).toIterable();
            for (ChatResponse response : responses) {
                providerSequence++;
                AiChatResponseChunk chunk = aiChatResponseExtractor.extractChunk(response);
                if (CharSequenceUtil.isNotBlank(chunk.getProviderRequestId())) {
                    providerRequestId = chunk.getProviderRequestId();
                }
                if (chunk.getTokenInput() != null) {
                    tokenInput = chunk.getTokenInput();
                }
                if (chunk.getTokenOutput() != null) {
                    tokenOutput = chunk.getTokenOutput();
                }
                if (chunk.getTokenTotal() != null) {
                    tokenTotal = chunk.getTokenTotal();
                }
                if (CharSequenceUtil.isNotBlank(chunk.getFinishReason())) {
                    finishReason = chunk.getFinishReason();
                }
                if (firstTokenAt == null && hasDelta(chunk)) {
                    firstTokenAt = LocalDateTime.now();
                }
                if (CharSequenceUtil.isNotBlank(chunk.getContentDelta())) {
                    fullContent.append(chunk.getContentDelta());
                }
                if (CharSequenceUtil.isNotBlank(chunk.getReasoningDelta())) {
                    fullReasoning.append(chunk.getReasoningDelta());
                }
                batchAccumulator.append(chunk.getContentDelta(), chunk.getReasoningDelta());
                if (batchAccumulator.shouldFlush(System.currentTimeMillis())) {
                    batchNo = flushBatch(
                            context,
                            emitter,
                            batchAccumulator,
                            batchNo,
                            providerSequence,
                            batchAccumulator.reachedCharThreshold() ? AiStreamFlushReasonEnum.CHAR_THRESHOLD : AiStreamFlushReasonEnum.TIME_WINDOW
                    );
                }
            }

            if (!batchAccumulator.hasPending() && fullContent.length() == 0 && fullReasoning.length() == 0) {
                throw new ServiceException(AiChatErrorCode.MODEL_RESPONSE_INVALID);
            }

            if (batchAccumulator.hasPending()) {
                batchNo = flushBatch(context, emitter, batchAccumulator, batchNo, providerSequence, AiStreamFlushReasonEnum.FINAL);
            }

            AiChatCompletionResult completionResult = new AiChatCompletionResult();
            completionResult.setProviderRequestId(providerRequestId);
            completionResult.setContent(fullContent.toString());
            completionResult.setReasoningContent(fullReasoning.toString());
            completionResult.setFinishReason(finishReason);
            completionResult.setTokenInput(tokenInput);
            completionResult.setTokenOutput(tokenOutput);
            completionResult.setTokenTotal(tokenTotal);
            completionResult.setFirstTokenAt(firstTokenAt != null ? firstTokenAt : LocalDateTime.now());
            completionResult.setCompletedAt(LocalDateTime.now());
            completionResult.setStreamBatchCount(batchNo);
            applySuccessOutcome(context, completionResult, AiChatRequestStatusEnum.SUCCESS.name());

            AiChatStreamEventVo endEvent = aiChatAssembler.toStreamEventVo(
                    context.getConversationId(),
                    context.getAssistantMessage().getId(),
                    context.getRequestId(),
                    batchNo
            );
            endEvent.setFinishReason(completionResult.getFinishReason());
            endEvent.setContentStatus(AiContentStatusEnum.DONE.name());
            endEvent.setFirstTokenAt(completionResult.getFirstTokenAt());
            endEvent.setCompletedAt(completionResult.getCompletedAt());
            endEvent.setTokenInput(completionResult.getTokenInput());
            endEvent.setTokenOutput(completionResult.getTokenOutput());
            endEvent.setTokenTotal(completionResult.getTokenTotal());
            sendStreamSuccessQuietly(emitter, "message-end", context.getRequestId(), endEvent);
            emitter.complete();
        } catch (Exception ex) {
            ServiceException serviceException = convertToServiceException(ex);
            if (batchAccumulator.hasPending()) {
                batchNo = flushBatch(context, emitter, batchAccumulator, batchNo, providerSequence, AiStreamFlushReasonEnum.ERROR);
            }
            AiChatCompletionResult completionResult = new AiChatCompletionResult();
            completionResult.setProviderRequestId(providerRequestId);
            completionResult.setContent(fullContent.toString());
            completionResult.setReasoningContent(fullReasoning.toString());
            completionResult.setFinishReason(finishReason);
            completionResult.setTokenInput(tokenInput);
            completionResult.setTokenOutput(tokenOutput);
            completionResult.setTokenTotal(tokenTotal);
            completionResult.setFirstTokenAt(firstTokenAt);
            completionResult.setCompletedAt(LocalDateTime.now());
            completionResult.setStreamBatchCount(batchNo);
            applyFailureOutcome(context, completionResult, serviceException, AiChatRequestStatusEnum.ERROR.name());

            AiChatStreamEventVo errorEvent = aiChatAssembler.toStreamEventVo(
                    context.getConversationId(),
                    context.getAssistantMessage().getId(),
                    context.getRequestId(),
                    batchNo
            );
            errorEvent.setContentStatus(AiContentStatusEnum.ERROR.name());
            errorEvent.setFinishReason(finishReason);
            sendStreamErrorQuietly(
                    emitter,
                    context.getRequestId(),
                    serviceException.getCode(),
                    serviceException.getMessage(),
                    errorEvent
            );
            emitter.complete();
        } finally {
            aiChatRedisCoordinator.releaseConversationLock(context.getConversationId());
        }
    }

    private int flushBatch(PreparedSendContext context,
                           SseEmitter emitter,
                           AiStreamBatchAccumulator batchAccumulator,
                           int currentBatchNo,
                           int providerSequence,
                           AiStreamFlushReasonEnum flushReason) {
        if (!batchAccumulator.hasPending()) {
            return currentBatchNo;
        }
        int nextBatchNo = currentBatchNo + 1;
        AiChatStreamBatchSnapshot snapshot = batchAccumulator.drain(System.currentTimeMillis());
        AiMessageStreamBatch batch = new AiMessageStreamBatch();
        batch.setId(SnowUtils.getSnowflakeNextId());
        batch.setAssistantMessageId(context.getAssistantMessage().getId());
        batch.setRequestId(context.getRequestId());
        batch.setBatchNo(nextBatchNo);
        batch.setContentDelta(snapshot.getContentDelta());
        batch.setReasoningDelta(snapshot.getReasoningDelta());
        batch.setFlushReason(flushReason.name());
        batch.setDeltaChars(snapshot.getDeltaChars());
        batch.setProviderSequence(providerSequence);
        aiMessageStreamBatchService.create(batch);

        AiChatStreamEventVo deltaEvent = aiChatAssembler.toStreamEventVo(
                context.getConversationId(),
                context.getAssistantMessage().getId(),
                context.getRequestId(),
                nextBatchNo
        );
        if (CharSequenceUtil.isNotBlank(snapshot.getContentDelta())) {
            deltaEvent.setContent(snapshot.getContentDelta());
            sendStreamSuccessQuietly(emitter, "message-delta", context.getRequestId(), deltaEvent);
        }
        if (CharSequenceUtil.isNotBlank(snapshot.getReasoningDelta())) {
            deltaEvent = aiChatAssembler.toStreamEventVo(
                    context.getConversationId(),
                    context.getAssistantMessage().getId(),
                    context.getRequestId(),
                    nextBatchNo
            );
            deltaEvent.setReasoningContent(snapshot.getReasoningDelta());
            sendStreamSuccessQuietly(emitter, "reasoning-delta", context.getRequestId(), deltaEvent);
        }
        return nextBatchNo;
    }

    private PreparedSendContext prepareSendContext(Long conversationId,
                                                   AiMessageSendDto sendDto,
                                                   AiChatRequestModeEnum requestMode) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        String requestId = defaultRequestId(sendDto.getRequestId());
        validateSendDto(sendDto);
        AiConversation conversation = requireConversation(conversationId);
        if (!AiConversationStatusEnum.ACTIVE.name().equals(conversation.getStatus())) {
            throw new ServiceException(AiChatErrorCode.CHAT_CONVERSATION_NOT_ACTIVE);
        }

        if (aiChatRequestService.getByConversationIdAndRequestId(conversationId, requestId) != null
                || !aiChatRedisCoordinator.markRequestIfAbsent(conversationId, requestId)) {
            throw new ServiceException(AiChatErrorCode.CHAT_REQUEST_DUPLICATE);
        }

        if (!aiChatRedisCoordinator.acquireConversationLock(conversationId, requestId)) {
            aiChatRedisCoordinator.releaseRequestMark(conversationId, requestId);
            throw new ServiceException(AiChatErrorCode.CHAT_GENERATING_CONFLICT);
        }

        try {
            return transactionTemplate.execute(status -> doPrepareSendContext(conversation, userId, requestId, sendDto, requestMode));
        } catch (RuntimeException ex) {
            aiChatRedisCoordinator.releaseConversationLock(conversationId);
            aiChatRedisCoordinator.releaseRequestMark(conversationId, requestId);
            throw ex;
        }
    }

    private PreparedSendContext doPrepareSendContext(AiConversation conversation,
                                                     Long userId,
                                                     String requestId,
                                                     AiMessageSendDto sendDto,
                                                     AiChatRequestModeEnum requestMode) {
        String modelCode = defaultModelCode(conversation.getCurrentModelCode());
        AiMessage userMessage = buildUserMessage(conversation.getId(), userId, requestId, modelCode, sendDto);
        aiMessageService.create(userMessage);

        AiMessage assistantMessage = buildAssistantPlaceholder(conversation.getId(), userId, requestId, modelCode, userMessage.getId());
        aiMessageService.create(assistantMessage);

        AiChatRequest chatRequest = buildChatRequest(conversation.getId(), userId, requestId, requestMode, modelCode, userMessage.getId(), assistantMessage.getId());
        try {
            aiChatRequestService.create(chatRequest);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(AiChatErrorCode.CHAT_REQUEST_DUPLICATE);
        }

        Prompt prompt = buildPrompt(conversation, userId, userMessage, modelCode, requestMode == AiChatRequestModeEnum.STREAM);
        updateConversationForPreparedRequest(conversation, requestId, modelCode);

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

    private Prompt buildPrompt(AiConversation conversation,
                               Long userId,
                               AiMessage userMessage,
                               String modelCode,
                               boolean stream) {
        List<Message> messages = new ArrayList<>();
        String systemPrompt = defaultSystemPrompt(conversation.getSystemPrompt());
        if (CharSequenceUtil.isNotBlank(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }

        AiConversationSummary summary = aiConversationSummaryService.getLatestReadySummary(conversation.getId(), userId);
        if (summary != null && CharSequenceUtil.isNotBlank(summary.getSummaryText())) {
            messages.add(new SystemMessage("以下是当前会话的历史摘要：\n" + summary.getSummaryText()));
        }

        List<AiMessage> historyMessages = aiMessageService.listRecentContextMessages(
                conversation.getId(),
                userId,
                userMessage.getId(),
                defaultMaxContextMessages(conversation.getMaxContextMessages())
        );
        Collections.reverse(historyMessages);
        for (AiMessage historyMessage : historyMessages) {
            Message promptMessage = toPromptMessage(historyMessage);
            if (promptMessage != null) {
                messages.add(promptMessage);
            }
        }
        messages.add(new UserMessage(userMessage.getContent()));
        return new Prompt(messages, buildChatOptions(conversation, modelCode, stream));
    }

    private Message toPromptMessage(AiMessage aiMessage) {
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

    private DashScopeChatOptions buildChatOptions(AiConversation conversation, String modelCode, boolean stream) {
        var builder = DashScopeChatOptions.builder()
                .withModel(modelCode)
                .withStream(stream)
                .withIncrementalOutput(stream)
                .withEnableThinking(isThinkingModel(modelCode));

        if (conversation.getMaxTokens() != null) {
            builder.withMaxToken(conversation.getMaxTokens());
        }
        if (conversation.getTemperature() != null) {
            builder.withTemperature(conversation.getTemperature().doubleValue());
        }
        return builder.build();
    }

    private void applySuccessOutcome(PreparedSendContext context,
                                     AiChatCompletionResult completionResult,
                                     String requestStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            AiMessage assistantUpdate = new AiMessage();
            assistantUpdate.setId(context.getAssistantMessage().getId());
            assistantUpdate.setProviderRequestId(completionResult.getProviderRequestId());
            assistantUpdate.setContent(completionResult.getContent());
            assistantUpdate.setReasoningContent(completionResult.getReasoningContent());
            assistantUpdate.setContentStatus(AiContentStatusEnum.DONE.name());
            assistantUpdate.setFinishReason(completionResult.getFinishReason());
            assistantUpdate.setTokenInput(completionResult.getTokenInput());
            assistantUpdate.setTokenOutput(completionResult.getTokenOutput());
            assistantUpdate.setTokenTotal(completionResult.getTokenTotal());
            assistantUpdate.setFirstTokenAt(completionResult.getFirstTokenAt());
            assistantUpdate.setCompletedAt(completionResult.getCompletedAt());
            assistantUpdate.setStreamBatchCount(defaultBatchCount(completionResult.getStreamBatchCount()));
            assistantUpdate.setErrorCode(null);
            assistantUpdate.setErrorMessage(null);
            aiMessageService.update(assistantUpdate);

            AiChatRequest requestUpdate = new AiChatRequest();
            requestUpdate.setId(context.getChatRequest().getId());
            requestUpdate.setProviderRequestId(completionResult.getProviderRequestId());
            requestUpdate.setStatus(requestStatus);
            requestUpdate.setFinishReason(completionResult.getFinishReason());
            requestUpdate.setTokenInput(completionResult.getTokenInput());
            requestUpdate.setTokenOutput(completionResult.getTokenOutput());
            requestUpdate.setTokenTotal(completionResult.getTokenTotal());
            requestUpdate.setFirstTokenAt(completionResult.getFirstTokenAt());
            requestUpdate.setCompletedAt(completionResult.getCompletedAt());
            requestUpdate.setErrorCode(null);
            requestUpdate.setErrorMessage(null);
            aiChatRequestService.update(requestUpdate);

            AiConversation conversationUpdate = new AiConversation();
            conversationUpdate.setId(context.getConversationId());
            conversationUpdate.setActiveRequestId(null);
            conversationUpdate.setLastErrorCode(null);
            conversationUpdate.setLastErrorMessage(null);
            conversationUpdate.setLastMessageAt(completionResult.getCompletedAt());
            aiConversationService.update(conversationUpdate);

            context.getAssistantMessage().setContentStatus(AiContentStatusEnum.DONE.name());
        });
    }

    private void applyFailureOutcome(PreparedSendContext context,
                                     AiChatCompletionResult completionResult,
                                     ServiceException serviceException,
                                     String requestStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            AiMessage assistantUpdate = new AiMessage();
            assistantUpdate.setId(context.getAssistantMessage().getId());
            assistantUpdate.setProviderRequestId(completionResult.getProviderRequestId());
            assistantUpdate.setContent(completionResult.getContent());
            assistantUpdate.setReasoningContent(completionResult.getReasoningContent());
            assistantUpdate.setContentStatus(AiContentStatusEnum.ERROR.name());
            assistantUpdate.setFinishReason(completionResult.getFinishReason());
            assistantUpdate.setTokenInput(completionResult.getTokenInput());
            assistantUpdate.setTokenOutput(completionResult.getTokenOutput());
            assistantUpdate.setTokenTotal(completionResult.getTokenTotal());
            assistantUpdate.setFirstTokenAt(completionResult.getFirstTokenAt());
            assistantUpdate.setCompletedAt(completionResult.getCompletedAt() != null ? completionResult.getCompletedAt() : LocalDateTime.now());
            assistantUpdate.setStreamBatchCount(defaultBatchCount(completionResult.getStreamBatchCount()));
            assistantUpdate.setErrorCode(String.valueOf(serviceException.getCode()));
            assistantUpdate.setErrorMessage(serviceException.getMessage());
            aiMessageService.update(assistantUpdate);

            AiChatRequest requestUpdate = new AiChatRequest();
            requestUpdate.setId(context.getChatRequest().getId());
            requestUpdate.setProviderRequestId(completionResult.getProviderRequestId());
            requestUpdate.setStatus(requestStatus);
            requestUpdate.setFinishReason(completionResult.getFinishReason());
            requestUpdate.setTokenInput(completionResult.getTokenInput());
            requestUpdate.setTokenOutput(completionResult.getTokenOutput());
            requestUpdate.setTokenTotal(completionResult.getTokenTotal());
            requestUpdate.setFirstTokenAt(completionResult.getFirstTokenAt());
            requestUpdate.setCompletedAt(completionResult.getCompletedAt() != null ? completionResult.getCompletedAt() : LocalDateTime.now());
            requestUpdate.setErrorCode(String.valueOf(serviceException.getCode()));
            requestUpdate.setErrorMessage(serviceException.getMessage());
            aiChatRequestService.update(requestUpdate);

            AiConversation conversationUpdate = new AiConversation();
            conversationUpdate.setId(context.getConversationId());
            conversationUpdate.setActiveRequestId(null);
            conversationUpdate.setLastErrorCode(String.valueOf(serviceException.getCode()));
            conversationUpdate.setLastErrorMessage(serviceException.getMessage());
            aiConversationService.update(conversationUpdate);

            context.getAssistantMessage().setContentStatus(AiContentStatusEnum.ERROR.name());
        });
    }

    private void updateConversationForPreparedRequest(AiConversation conversation, String requestId, String modelCode) {
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setActiveRequestId(requestId);
        update.setCurrentModelCode(modelCode);
        update.setLastMessageAt(LocalDateTime.now());
        update.setMessageCount(defaultNumber(conversation.getMessageCount()) + 2);
        update.setRoundCount(defaultNumber(conversation.getRoundCount()) + 1);
        update.setLastErrorCode(null);
        update.setLastErrorMessage(null);
        aiConversationService.update(update);
        conversation.setMessageCount(update.getMessageCount());
        conversation.setRoundCount(update.getRoundCount());
        conversation.setCurrentModelCode(modelCode);
        conversation.setActiveRequestId(requestId);
    }

    private void markRequestStatus(PreparedSendContext context, String status) {
        AiChatRequest update = new AiChatRequest();
        update.setId(context.getChatRequest().getId());
        update.setStatus(status);
        aiChatRequestService.update(update);
    }

    private AiMessage buildUserMessage(Long conversationId,
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

    private AiMessage buildAssistantPlaceholder(Long conversationId,
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

    private AiChatRequest buildChatRequest(Long conversationId,
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

    private void validateSendDto(AiMessageSendDto sendDto) {
        String messageType = defaultMessageType(sendDto.getMessageType());
        if (!AiMessageTypeEnum.TEXT.name().equals(messageType)) {
            throw new ServiceException(AiChatErrorCode.CHAT_MESSAGE_TYPE_UNSUPPORTED);
        }
    }

    private void validateCompletion(AiChatCompletionResult completionResult) {
        if (completionResult == null) {
            throw new ServiceException(AiChatErrorCode.MODEL_RESPONSE_INVALID);
        }
        if (CharSequenceUtil.isBlank(completionResult.getContent())
                && CharSequenceUtil.isBlank(completionResult.getReasoningContent())) {
            throw new ServiceException(AiChatErrorCode.MODEL_RESPONSE_INVALID);
        }
    }

    private boolean hasDelta(AiChatResponseChunk chunk) {
        return chunk != null && (CharSequenceUtil.isNotBlank(chunk.getContentDelta())
                || CharSequenceUtil.isNotBlank(chunk.getReasoningDelta()));
    }

    private String defaultConversationTitle(String title) {
        return CharSequenceUtil.isBlank(title) ? "新对话" : title.trim();
    }

    private String defaultSystemPrompt(String systemPrompt) {
        return CharSequenceUtil.isBlank(systemPrompt) ? aiChatProperties.getDefaultSystemPrompt() : systemPrompt.trim();
    }

    private String defaultModelCode(String modelCode) {
        return CharSequenceUtil.isBlank(modelCode) ? aiChatProperties.getDashscopeChatModel() : modelCode.trim();
    }

    private Integer defaultMaxContextMessages(Integer maxContextMessages) {
        return maxContextMessages == null || maxContextMessages <= 0
                ? aiChatProperties.getMaxContextMessages()
                : maxContextMessages;
    }

    private String defaultRequestId(String requestId) {
        return CharSequenceUtil.isBlank(requestId) ? IdUtil.fastSimpleUUID() : requestId.trim();
    }

    private String defaultMessageType(String messageType) {
        return CharSequenceUtil.isBlank(messageType) ? AiMessageTypeEnum.TEXT.name() : messageType.trim().toUpperCase();
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer defaultBatchCount(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isThinkingModel(String modelCode) {
        return CharSequenceUtil.isNotBlank(modelCode) && modelCode.startsWith("qwq-");
    }

    private AiConversation requireConversation(Long conversationId) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = aiConversationService.getByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new ServiceException(AiChatErrorCode.CHAT_CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    private AiChatCompletionResult emptyCompletionResult() {
        AiChatCompletionResult completionResult = new AiChatCompletionResult();
        completionResult.setCompletedAt(LocalDateTime.now());
        completionResult.setStreamBatchCount(0);
        return completionResult;
    }

    private ServiceException convertToServiceException(Exception ex) {
        if (ex instanceof ServiceException serviceException) {
            return serviceException;
        }
        Throwable rootCause = rootCause(ex);
        if (ex instanceof HttpClientErrorException httpClientErrorException) {
            if (httpClientErrorException.getStatusCode().value() == 401
                    || httpClientErrorException.getStatusCode().value() == 403) {
                return new ServiceException(AiChatErrorCode.MODEL_AUTH_ERROR);
            }
            if (httpClientErrorException.getStatusCode().value() == 429) {
                return new ServiceException(AiChatErrorCode.MODEL_RATE_LIMIT);
            }
        }
        if (rootCause instanceof SocketTimeoutException || containsIgnoreCase(ex.getMessage(), "timeout")) {
            return new ServiceException(AiChatErrorCode.MODEL_TIMEOUT);
        }
        if (rootCause instanceof UnknownHostException
                || rootCause instanceof ConnectException
                || rootCause instanceof ResourceAccessException
                || containsIgnoreCase(ex.getMessage(), "connection refused")) {
            return new ServiceException(AiChatErrorCode.MODEL_NETWORK_ERROR);
        }
        if (containsIgnoreCase(ex.getMessage(), "401") || containsIgnoreCase(ex.getMessage(), "403")) {
            return new ServiceException(AiChatErrorCode.MODEL_AUTH_ERROR);
        }
        if (containsIgnoreCase(ex.getMessage(), "429")) {
            return new ServiceException(AiChatErrorCode.MODEL_RATE_LIMIT);
        }
        return new ServiceException(AiChatErrorCode.MODEL_PROVIDER_ERROR.getCode(), fallbackMessage(ex.getMessage(), AiChatErrorCode.MODEL_PROVIDER_ERROR));
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private boolean containsIgnoreCase(String source, String target) {
        return CharSequenceUtil.isNotBlank(source) && source.toLowerCase().contains(target.toLowerCase());
    }

    private String fallbackMessage(String message, ErrorCode errorCode) {
        return CharSequenceUtil.isBlank(message) ? errorCode.getMsg() : message;
    }

    private void sendStreamSuccessQuietly(SseEmitter emitter, String event, String requestId, Object content) {
        try {
            aiChatSseWriter.sendSuccess(emitter, event, requestId, content);
        } catch (IOException ioException) {
            LOG.warn("SSE success event send failed, event={}, requestId={}", event, requestId, ioException);
        }
    }

    private void sendStreamErrorQuietly(SseEmitter emitter,
                                        String requestId,
                                        Integer code,
                                        String message,
                                        Object content) {
        try {
            aiChatSseWriter.sendError(emitter, requestId, code, message, content);
        } catch (IOException ioException) {
            LOG.warn("SSE error event send failed, requestId={}", requestId, ioException);
        }
    }
}
