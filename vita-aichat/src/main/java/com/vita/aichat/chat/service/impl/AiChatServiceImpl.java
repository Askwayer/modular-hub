package com.vita.aichat.chat.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.client.AiChatClient;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.model.context.AiChatCapacityPermit;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.context.ReservedSendContext;
import com.vita.aichat.chat.model.request.StreamRequestState;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.model.result.StreamProcessResult;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.support.*;
import com.vita.aichat.chat.vo.*;
import com.vita.aichat.enums.*;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.model.LoginUserInfoModel;
import com.vita.core.page.PageResponse;
import com.vita.utils.SnowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * AI Chat 聚合服务实现。
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final IAiConversationService aiConversationService;
    private final IAiMessageService aiMessageService;
    private final IAiConversationSummaryService aiConversationSummaryService;
    private final IAiChatRequestService aiChatRequestService;
    private final AiChatClient aiChatClient;
    private final AiChatProperty aiChatProperty;
    private final AiChatAssembler aiChatAssembler;
    private final AiChatConcurrencyCoordinator aiChatConcurrencyCoordinator;
    private final AiChatSseWriter aiChatSseWriter;
    private final Executor aiChatExecutor;
    private final AiChatStreamProcessor aiChatStreamProcessor;
    private final AiChatOutcomeApplier aiChatOutcomeApplier;
    private final AiChatRequestPreparer aiChatRequestPreparer;
    private final AiChatExceptionConverter aiChatExceptionConverter;
    private final AiChatSummaryDispatcher aiChatSummaryDispatcher;
    private final AiChatServiceImpl self;

    public AiChatServiceImpl(IAiConversationService aiConversationService,
                             IAiMessageService aiMessageService,
                             IAiConversationSummaryService aiConversationSummaryService,
                             IAiChatRequestService aiChatRequestService,
                             AiChatClient aiChatClient,
                             AiChatProperty aiChatProperty,
                             AiChatAssembler aiChatAssembler,
                             AiChatConcurrencyCoordinator aiChatConcurrencyCoordinator,
                             AiChatSseWriter aiChatSseWriter,
                             @Qualifier("aiChatTaskExecutor") Executor aiChatExecutor,
                             AiChatStreamProcessor aiChatStreamProcessor,
                             AiChatOutcomeApplier aiChatOutcomeApplier,
                             AiChatRequestPreparer aiChatRequestPreparer,
                             AiChatExceptionConverter aiChatExceptionConverter,
                             AiChatSummaryDispatcher aiChatSummaryDispatcher,
                             @Lazy AiChatServiceImpl self) {
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiChatClient = aiChatClient;
        this.aiChatProperty = aiChatProperty;
        this.aiChatAssembler = aiChatAssembler;
        this.aiChatConcurrencyCoordinator = aiChatConcurrencyCoordinator;
        this.aiChatSseWriter = aiChatSseWriter;
        this.aiChatExecutor = aiChatExecutor;
        this.aiChatStreamProcessor = aiChatStreamProcessor;
        this.aiChatOutcomeApplier = aiChatOutcomeApplier;
        this.aiChatRequestPreparer = aiChatRequestPreparer;
        this.aiChatExceptionConverter = aiChatExceptionConverter;
        this.aiChatSummaryDispatcher = aiChatSummaryDispatcher;
        this.self = self;
    }

    @Override
    public Long createConversation(AiConversationCreateDto createDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = new AiConversation();
        conversation.setId(SnowUtils.getSnowflakeNextId());
        conversation.setUserId(userId);
        conversation.setTitle(aiChatRequestPreparer.generateConversationTitle(createDto.getTitle()));
        conversation.setTitleSource(AiTitleSourceEnum.AUTO.name());
        conversation.setStatus(AiConversationStatusEnum.ACTIVE.name());
        conversation.setPinned((byte) 0);
        conversation.setMessageCount(0);
        conversation.setRoundCount(0);
        conversation.setSummaryVersion(0);
        conversation.setSummaryStatus(AiSummaryStatusEnum.NONE.name());
        conversation.setCurrentProviderCode(aiChatRequestPreparer.generateProviderCode(createDto.getProviderCode()));
        conversation.setCurrentModelCode(aiChatRequestPreparer.generateModelCode(createDto.getModelCode()));
        conversation.setSystemPrompt(aiChatRequestPreparer.generateSystemPrompt(createDto.getSystemPrompt()));
        conversation.setTemperature(createDto.getTemperature());
        conversation.setMaxTokens(createDto.getMaxTokens());
        conversation.setMaxContextMessages(aiChatRequestPreparer.generateMaxContextMessages(createDto.getMaxContextMessages()));
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
        return aiChatAssembler.toConversationDetailVo(aiChatRequestPreparer.requireConversation(conversationId));
    }

    @Override
    public void renameConversation(Long conversationId, AiConversationTitleUpdateDto updateDto) {
        AiConversation conversation = aiChatRequestPreparer.requireConversation(conversationId);
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setTitle(updateDto.getTitle().trim());
        update.setTitleSource(AiTitleSourceEnum.MANUAL.name());
        aiConversationService.update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = aiChatRequestPreparer.requireConversation(conversationId);

        aiChatRequestService.deleteByConversationIdAndUserId(conversation.getId(), userId);
        aiConversationSummaryService.deleteByConversationIdAndUserId(conversation.getId(), userId);
        aiMessageService.deleteByConversationIdAndUserId(conversation.getId(), userId);
        aiConversationService.deleteByIdAndUserId(conversation.getId(), userId);
    }

    @Override
    public PageResponse<AiMessagePageVo> pageMessages(Long conversationId, AiMessagePageQueryDto queryDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        aiChatRequestPreparer.requireConversation(conversationId);
        PageResponse<AiMessage> pageResponse = aiMessageService.pageByConversationIdAndUserId(queryDto, conversationId, userId);
        PageResponse<AiMessagePageVo> response = new PageResponse<>();
        response.setTotal(pageResponse.getTotal());
        response.setList(pageResponse.getList().stream().map(aiChatAssembler::toMessagePageVo).toList());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSendVo sendMessage(Long conversationId, AiMessageSendDto sendDto) {
        ReservedSendContext reservedContext = aiChatRequestPreparer.reserveSendContext(conversationId, sendDto);
        AiChatCapacityPermit capacityPermit = null;
        PreparedSendContext context = null;
        boolean conversationLockAcquired = false;
        try {
            capacityPermit = aiChatConcurrencyCoordinator.acquireCapacityForSync();
            if (!aiChatConcurrencyCoordinator.acquireConversationLock(conversationId)) {
                throw new ServiceException(GlobalErrorCode.CHAT_GENERATING_CONFLICT);
            }
            conversationLockAcquired = true;
            context = prepareSendContext(reservedContext, AiChatRequestModeEnum.SYNC, capacityPermit);
            AiChatResponse response = aiChatClient.chat(context.getAiChatRequest());
            AiChatCompletionResult completionResult = toCompletionResult(response);
            validateCompletion(completionResult);
            LocalDateTime completedAt = LocalDateTime.now();
            completionResult = AiChatCompletionResult.builder()
                    .providerRequestId(completionResult.getProviderRequestId())
                    .content(completionResult.getContent())
                    .reasoningContent(completionResult.getReasoningContent())
                    .finishReason(completionResult.getFinishReason())
                    .tokenInput(completionResult.getTokenInput())
                    .tokenOutput(completionResult.getTokenOutput())
                    .tokenTotal(completionResult.getTokenTotal())
                    .firstTokenAt(completionResult.getFirstTokenAt() != null ? completionResult.getFirstTokenAt() : completedAt)
                    .completedAt(completedAt)
                    .streamBatchCount(0)
                    .build();
            aiChatOutcomeApplier.applySuccess(context, completionResult, AiChatRequestStatusEnum.SUCCESS.name());
            aiChatSummaryDispatcher.publishRoundSummary(context);
            return aiChatAssembler.toSendVo(context.getUserMessage(), context.getAssistantMessage(), completionResult);
        } catch (Exception ex) {
            if (context == null) {
                aiChatRequestPreparer.releaseRequestReservation(reservedContext);
                throw ex instanceof ServiceException serviceException
                        ? serviceException
                        : aiChatExceptionConverter.convertToServiceException(ex);
            }
            ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(ex);
            aiChatOutcomeApplier.applyFailure(context, emptyCompletionResult(), serviceException.getCode(), serviceException.getMessage(), AiChatRequestStatusEnum.ERROR.name());
            throw serviceException;
        } finally {
            if (conversationLockAcquired) {
                aiChatConcurrencyCoordinator.releaseConversationLock(conversationId);
            }
            if (capacityPermit != null) {
                aiChatConcurrencyCoordinator.releaseActivePermit(capacityPermit.getActivePermitId());
            }
        }
    }

    @Override
    public SseEmitter sendMessageStream(Long conversationId, AiMessageSendDto sendDto) {
        if (!Boolean.TRUE.equals(aiChatProperty.getStreamEnabled())) {
            throw new ServiceException(GlobalErrorCode.CHAT_STREAM_DISABLED);
        }

        ReservedSendContext reservedContext = aiChatRequestPreparer.reserveSendContext(conversationId, sendDto);
        SseEmitter emitter = aiChatSseWriter.createEmitter();
        LoginUserInfoModel currentUserInfo = LoginUserInfoModelContext.getLoginUserInfo();
        StreamRequestState streamState = new StreamRequestState(reservedContext, emitter);
        registerStreamEmitterCallbacks(streamState);

        try {
            AiChatCapacityPermit capacityPermit = aiChatConcurrencyCoordinator.tryAcquireCapacityForStream();
            if (capacityPermit != null) {
                submitStreamTask(streamState, currentUserInfo, capacityPermit);
                return emitter;
            }

            String waitingPermitId = aiChatConcurrencyCoordinator.tryAcquireWaitingPermit();
            streamState.setWaitingPermitId(waitingPermitId);
            sendWaitingEvent(streamState);
            aiChatConcurrencyCoordinator.acquireActivePermitAsync().onComplete((activePermitId, throwable) ->
                    handleAsyncCapacityReady(streamState, currentUserInfo, activePermitId, throwable));
        } catch (ServiceException serviceException) {
            aiChatRequestPreparer.releaseRequestReservation(reservedContext);
            sendStreamErrorQuietly(
                    emitter,
                    reservedContext.getRequestId(),
                    serviceException.getCode(),
                    serviceException.getMessage(),
                    buildStreamErrorEvent(reservedContext)
            );
            emitter.complete();
        }
        return emitter;
    }

    private void handleAsyncCapacityReady(StreamRequestState streamState,
                                          LoginUserInfoModel currentUserInfo,
                                          String activePermitId,
                                          Throwable throwable) {
        try {
            if (streamState.getWaitingPermitId() != null) {
                aiChatConcurrencyCoordinator.releaseWaitingPermit(streamState.getWaitingPermitId());
                streamState.setWaitingPermitId(null);
            }

            if (streamState.getClientClosed().get() && !streamState.getWorkerSubmitted().get()) {
                if (activePermitId != null) {
                    aiChatConcurrencyCoordinator.releaseActivePermit(activePermitId);
                }
                aiChatRequestPreparer.releaseRequestReservation(streamState.getReservedContext());
                return;
            }

            if (throwable != null) {
                ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(toException(throwable));
                aiChatRequestPreparer.releaseRequestReservation(streamState.getReservedContext());
                sendStreamErrorQuietly(
                        streamState.getEmitter(),
                        streamState.getReservedContext().getRequestId(),
                        serviceException.getCode(),
                        serviceException.getMessage(),
                        buildStreamErrorEvent(streamState.getReservedContext())
                );
                streamState.getEmitter().complete();
                return;
            }

            if (activePermitId == null) {
                ServiceException serviceException = new ServiceException(GlobalErrorCode.CHAT_CAPACITY_WAIT_TIMEOUT);
                aiChatRequestPreparer.releaseRequestReservation(streamState.getReservedContext());
                sendStreamErrorQuietly(
                        streamState.getEmitter(),
                        streamState.getReservedContext().getRequestId(),
                        serviceException.getCode(),
                        serviceException.getMessage(),
                        buildStreamErrorEvent(streamState.getReservedContext())
                );
                streamState.getEmitter().complete();
                return;
            }

            AiChatCapacityPermit capacityPermit = new AiChatCapacityPermit(activePermitId, true);
            submitStreamTask(streamState, currentUserInfo, capacityPermit);
        } catch (Exception ex) {
            ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(toException(ex));
            aiChatRequestPreparer.releaseRequestReservation(streamState.getReservedContext());
            sendStreamErrorQuietly(
                    streamState.getEmitter(),
                    streamState.getReservedContext().getRequestId(),
                    serviceException.getCode(),
                    serviceException.getMessage(),
                    buildStreamErrorEvent(streamState.getReservedContext())
            );
            streamState.getEmitter().complete();
        }
    }

    private void submitStreamTask(StreamRequestState streamState,
                                  LoginUserInfoModel currentUserInfo,
                                  AiChatCapacityPermit capacityPermit) {
        streamState.getWorkerSubmitted().set(true);
        streamState.setActivePermitId(capacityPermit.getActivePermitId());

        try {
            aiChatExecutor.execute(() -> {
                try {
                    LoginUserInfoModelContext.setLoginUserInfo(currentUserInfo);
                    self.processStreamWithReservation(streamState, capacityPermit);
                } finally {
                    LoginUserInfoModelContext.removeLoginUserInfo();
                }
            });
        } catch (RejectedExecutionException ex) {
            ServiceException serviceException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE);
            aiChatConcurrencyCoordinator.releaseActivePermit(capacityPermit.getActivePermitId());
            aiChatRequestPreparer.releaseRequestReservation(streamState.getReservedContext());
            sendStreamErrorQuietly(
                    streamState.getEmitter(),
                    streamState.getReservedContext().getRequestId(),
                    serviceException.getCode(),
                    serviceException.getMessage(),
                    buildStreamErrorEvent(streamState.getReservedContext())
            );
            streamState.getEmitter().complete();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processStreamWithReservation(StreamRequestState streamState,
                                             AiChatCapacityPermit capacityPermit) {
        ReservedSendContext reservedContext = streamState.getReservedContext();
        boolean conversationLockAcquired = false;
        PreparedSendContext context = null;

        try {
            if (streamState.getClientClosed().get()) {
                aiChatRequestPreparer.releaseRequestReservation(reservedContext);
                return;
            }
            if (!aiChatConcurrencyCoordinator.acquireConversationLock(reservedContext.getConversationId())) {
                aiChatRequestPreparer.releaseRequestReservation(reservedContext);
                ServiceException serviceException = new ServiceException(GlobalErrorCode.CHAT_GENERATING_CONFLICT);
                sendStreamErrorQuietly(
                        streamState.getEmitter(),
                        reservedContext.getRequestId(),
                        serviceException.getCode(),
                        serviceException.getMessage(),
                        buildStreamErrorEvent(reservedContext)
                );
                streamState.getEmitter().complete();
                return;
            }
            conversationLockAcquired = true;

            if (streamState.getClientClosed().get()) {
                aiChatRequestPreparer.releaseRequestReservation(reservedContext);
                return;
            }

            context = prepareSendContext(reservedContext, AiChatRequestModeEnum.STREAM, capacityPermit);
            streamState.getRequestPrepared().set(true);
            sendStartEvent(context, streamState.getEmitter());
            markRequestStatus(context, AiChatRequestStatusEnum.STREAMING.name());

            Flux<AiStreamChunk> streamFlux = aiChatClient.streamChat(context.getAiChatRequest());
            StreamProcessResult streamResult = aiChatStreamProcessor.process(context, streamState.getEmitter(), streamFlux);

            AiChatCompletionResult completionResult = AiChatCompletionResult.builder()
                    .providerRequestId(streamResult.getProviderRequestId())
                    .content(streamResult.getContent())
                    .reasoningContent(streamResult.getReasoningContent())
                    .finishReason(streamResult.getFinishReason())
                    .tokenInput(streamResult.getTokenInput())
                    .tokenOutput(streamResult.getTokenOutput())
                    .tokenTotal(streamResult.getTokenTotal())
                    .firstTokenAt(streamResult.getFirstTokenAt())
                    .completedAt(streamResult.getCompletedAt())
                    .streamBatchCount(streamResult.getStreamBatchCount())
                    .build();

            aiChatOutcomeApplier.applySuccess(context, completionResult, AiChatRequestStatusEnum.SUCCESS.name());
            aiChatSummaryDispatcher.publishRoundSummary(context);
            aiChatStreamProcessor.sendEndEvent(context, streamState.getEmitter(), streamResult);
            streamState.getEmitter().complete();
        } catch (Exception ex) {
            if (context == null) {
                ServiceException serviceException = ex instanceof ServiceException service
                        ? service
                        : aiChatExceptionConverter.convertToServiceException(ex);
                aiChatRequestPreparer.releaseRequestReservation(reservedContext);
                sendStreamErrorQuietly(
                        streamState.getEmitter(),
                        reservedContext.getRequestId(),
                        serviceException.getCode(),
                        serviceException.getMessage(),
                        buildStreamErrorEvent(reservedContext)
                );
                streamState.getEmitter().complete();
                return;
            }
            ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(ex);
            AiChatCompletionResult completionResult = extractCompletionFromException();
            aiChatOutcomeApplier.applyFailure(context, completionResult, serviceException.getCode(), serviceException.getMessage(), AiChatRequestStatusEnum.ERROR.name());
            aiChatStreamProcessor.sendErrorEvent(context, streamState.getEmitter(), serviceException.getCode(), serviceException.getMessage(), null);
            streamState.getEmitter().complete();
        } finally {
            if (conversationLockAcquired) {
                aiChatConcurrencyCoordinator.releaseConversationLock(reservedContext.getConversationId());
            }
            aiChatConcurrencyCoordinator.releaseActivePermit(capacityPermit.getActivePermitId());
        }
    }

    private AiChatCompletionResult extractCompletionFromException() {
        return emptyCompletionResult();
    }

    private PreparedSendContext prepareSendContext(ReservedSendContext reservedContext,
                                                   AiChatRequestModeEnum requestMode,
                                                   AiChatCapacityPermit capacityPermit) {
        PreparedSendContext context = aiChatRequestPreparer.prepareSendContext(reservedContext, requestMode);
        context.setActivePermitId(capacityPermit.getActivePermitId());
        context.setWaited(capacityPermit.isWaited());
        return context;
    }

    private void markRequestStatus(PreparedSendContext context, String status) {
        AiChatRequest update = new AiChatRequest();
        update.setId(context.getChatRequest().getId());
        update.setStatus(status);
        aiChatRequestService.update(update);
    }

    private AiChatCompletionResult toCompletionResult(AiChatResponse response) {
        return AiChatCompletionResult.builder()
                .providerRequestId(response.getProviderRequestId())
                .content(response.getContent())
                .reasoningContent(response.getReasoningContent())
                .finishReason(response.getFinishReason())
                .tokenInput(response.getTokenInput())
                .tokenOutput(response.getTokenOutput())
                .tokenTotal(response.getTokenTotal())
                .firstTokenAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .streamBatchCount(0)
                .build();
    }

    private void validateCompletion(AiChatCompletionResult completionResult) {
        if (completionResult == null) {
            throw new ServiceException(GlobalErrorCode.MODEL_RESPONSE_INVALID);
        }
        if (CharSequenceUtil.isBlank(completionResult.getContent())
                && CharSequenceUtil.isBlank(completionResult.getReasoningContent())) {
            throw new ServiceException(GlobalErrorCode.MODEL_RESPONSE_INVALID);
        }
    }

    private AiChatCompletionResult emptyCompletionResult() {
        return AiChatCompletionResult.builder()
                .completedAt(LocalDateTime.now())
                .streamBatchCount(0)
                .build();
    }

    /**
     * 注册 SSE 发射器的生命周期回调，在流式响应完成、超时或异常时统一触发客户端断开清理逻辑
     *
     * @param streamState 流式请求上下文状态对象
     */
    private void registerStreamEmitterCallbacks(StreamRequestState streamState) {
        streamState.getEmitter().onCompletion(() -> onStreamEmitterClosed(streamState));
        streamState.getEmitter().onTimeout(() -> onStreamEmitterClosed(streamState));
        streamState.getEmitter().onError(ex -> onStreamEmitterClosed(streamState));
    }

    private void onStreamEmitterClosed(StreamRequestState streamState) {
        streamState.getClientClosed().set(true);
        if (!streamState.getWorkerSubmitted().get()) {
            if (streamState.getWaitingPermitId() != null) {
                aiChatConcurrencyCoordinator.releaseWaitingPermit(streamState.getWaitingPermitId());
                streamState.setWaitingPermitId(null);
            }
            if (streamState.getActivePermitId() != null) {
                aiChatConcurrencyCoordinator.releaseActivePermit(streamState.getActivePermitId());
                streamState.setActivePermitId(null);
            }
            if (!streamState.getRequestPrepared().get()) {
                aiChatRequestPreparer.releaseRequestReservation(streamState.getReservedContext());
            }
        }
    }

    private void sendWaitingEvent(StreamRequestState streamState) {
        AiChatStreamEventVo waitingEvent = aiChatAssembler.toStreamEventVo(
                streamState.getReservedContext().getConversationId(),
                null,
                streamState.getReservedContext().getRequestId(),
                0
        );
        waitingEvent.setContentStatus(AiContentStatusEnum.WAITING.name());
        waitingEvent.setContent("当前并发较高，已进入等待队列");
        sendStreamSuccessQuietly(streamState.getEmitter(), "waiting", streamState.getReservedContext().getRequestId(), waitingEvent);
    }

    private void sendStartEvent(PreparedSendContext context, SseEmitter emitter) {
        AiChatStreamEventVo startEvent = aiChatAssembler.toStreamEventVo(
                context.getConversationId(),
                context.getAssistantMessage().getId(),
                context.getRequestId(),
                0
        );
        startEvent.setContentStatus(AiContentStatusEnum.GENERATING.name());
        sendStreamSuccessQuietly(emitter, "message-start", context.getRequestId(), startEvent);
    }

    private AiChatStreamEventVo buildStreamErrorEvent(ReservedSendContext reservedContext) {
        AiChatStreamEventVo errorEvent = aiChatAssembler.toStreamEventVo(
                reservedContext.getConversationId(),
                null,
                reservedContext.getRequestId(),
                0
        );
        errorEvent.setContentStatus(AiContentStatusEnum.ERROR.name());
        return errorEvent;
    }

    private Exception toException(Throwable throwable) {
        if (throwable instanceof Exception exception) {
            return exception;
        }
        return new RuntimeException(throwable);
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
