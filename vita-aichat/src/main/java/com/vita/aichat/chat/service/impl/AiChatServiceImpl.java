package com.vita.aichat.chat.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.client.AiChatClient;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.aiRoundMemory.service.IAiRoundMemoryService;
import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.model.context.*;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.model.result.StreamProcessResult;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.support.*;
import com.vita.aichat.chat.vo.*;
import com.vita.aichat.enums.*;
import com.vita.core.CommonStreamResult;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.utils.SnowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI Chat 聚合服务实现。
 */
@Service
public class AiChatServiceImpl implements IAiChatService {
    private final IAiConversationService aiConversationService;
    private final IAiMessageService aiMessageService;
    private final IAiConversationSummaryService aiConversationSummaryService;
    private final IAiChatRequestService aiChatRequestService;
    private final IAiRoundMemoryService aiRoundMemoryService;
    private final AiChatClient aiChatClient;
    private final AiChatProperty aiChatProperty;
    private final AiChatAssembler aiChatAssembler;
    private final AiChatConcurrencyCoordinator aiChatConcurrencyCoordinator;
    private final AiChatStreamProcessor aiChatStreamProcessor;
    private final AiChatOutcomeApplier aiChatOutcomeApplier;
    private final AiChatRequestPreparer aiChatRequestPreparer;
    private final AiChatExceptionConverter aiChatExceptionConverter;
    private final AiChatSummaryDispatcher aiChatSummaryDispatcher;
    private final TransactionTemplate transactionTemplate;
    private final AiChatReactiveBridge aiChatReactiveBridge;

    public AiChatServiceImpl(IAiConversationService aiConversationService,
                             IAiMessageService aiMessageService,
                             IAiConversationSummaryService aiConversationSummaryService,
                             IAiChatRequestService aiChatRequestService,
                             IAiRoundMemoryService aiRoundMemoryService,
                             AiChatClient aiChatClient,
                             AiChatProperty aiChatProperty,
                             AiChatAssembler aiChatAssembler,
                             AiChatConcurrencyCoordinator aiChatConcurrencyCoordinator,
                             AiChatStreamProcessor aiChatStreamProcessor,
                             AiChatOutcomeApplier aiChatOutcomeApplier,
                             AiChatRequestPreparer aiChatRequestPreparer,
                             AiChatExceptionConverter aiChatExceptionConverter,
                             AiChatSummaryDispatcher aiChatSummaryDispatcher,
                             TransactionTemplate transactionTemplate,
                             AiChatReactiveBridge aiChatReactiveBridge) {
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiRoundMemoryService = aiRoundMemoryService;
        this.aiChatClient = aiChatClient;
        this.aiChatProperty = aiChatProperty;
        this.aiChatAssembler = aiChatAssembler;
        this.aiChatConcurrencyCoordinator = aiChatConcurrencyCoordinator;
        this.aiChatStreamProcessor = aiChatStreamProcessor;
        this.aiChatOutcomeApplier = aiChatOutcomeApplier;
        this.aiChatRequestPreparer = aiChatRequestPreparer;
        this.aiChatExceptionConverter = aiChatExceptionConverter;
        this.aiChatSummaryDispatcher = aiChatSummaryDispatcher;
        this.transactionTemplate = transactionTemplate;
        this.aiChatReactiveBridge = aiChatReactiveBridge;
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
        aiRoundMemoryService.deleteByConversationIdAndUserId(conversation.getId(), userId);
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
    public Flux<CommonStreamResult<AiChatStreamEventVo>> sendMessageStream(Long conversationId, AiMessageSendDto sendDto) {
        if (!Boolean.TRUE.equals(aiChatProperty.getStreamEnabled())) {
            throw new ServiceException(GlobalErrorCode.CHAT_STREAM_DISABLED);
        }
        return Flux.deferContextual(contextView -> {
            AiChatReactiveRequestContext requestContext = aiChatReactiveBridge.currentRequestContext(contextView);
            return aiChatReactiveBridge.blocking(requestContext,
                            () -> aiChatRequestPreparer.reserveSendContext(conversationId, sendDto))
                    .flatMapMany(reservedContext ->
                            startReactiveStream(requestContext, new ReactiveStreamSession(reservedContext)));
        });
    }

    private Flux<CommonStreamResult<AiChatStreamEventVo>> startReactiveStream(AiChatReactiveRequestContext requestContext,
                                                                              ReactiveStreamSession streamSession) {
        return aiChatReactiveBridge.blocking(requestContext, aiChatConcurrencyCoordinator::tryAcquireCapacityForStream)
                .flatMapMany(capacityPermit -> {
                    if (capacityPermit != null) {
                        return executeReactiveStream(requestContext, streamSession, capacityPermit);
                    }
                    return enqueueAndAwaitCapacity(requestContext, streamSession);
                })
                .onErrorResume(ex -> emitBootstrapError(requestContext, streamSession, ex));
    }

    private Flux<CommonStreamResult<AiChatStreamEventVo>> enqueueAndAwaitCapacity(AiChatReactiveRequestContext requestContext,
                                                                                   ReactiveStreamSession streamSession) {
        return aiChatReactiveBridge.blocking(requestContext, aiChatConcurrencyCoordinator::tryAcquireWaitingPermit)
                .flatMapMany(waitingPermitId -> {
                    streamSession.setWaitingPermitId(waitingPermitId);
                    return Flux.concat(
                            Flux.just(buildWaitingEvent(streamSession)),
                            awaitActivePermit(requestContext, streamSession)
                                    .flatMapMany(activePermitId -> executeReactiveStream(
                                            requestContext,
                                            streamSession,
                                            new AiChatCapacityPermit(activePermitId, true)
                                    ))
                    );
                });
    }

    private Mono<String> awaitActivePermit(AiChatReactiveRequestContext requestContext,
                                           ReactiveStreamSession streamSession) {
        return Mono.create(sink -> {
            org.redisson.api.RFuture<String> future = aiChatConcurrencyCoordinator.acquireActivePermitAsync();
            future.onComplete((activePermitId, throwable) -> {
                aiChatReactiveBridge.fireAndForget(requestContext,
                        () -> releaseWaitingPermitIfPresent(streamSession));
                if (throwable != null) {
                    sink.error(toException(throwable));
                    return;
                }
                if (activePermitId == null) {
                    sink.error(new ServiceException(GlobalErrorCode.CHAT_CAPACITY_WAIT_TIMEOUT));
                    return;
                }
                streamSession.setActivePermitId(activePermitId);
                streamSession.setWaited(true);
                sink.success(activePermitId);
            });
            sink.onCancel(() -> {
                future.cancel(true);
                aiChatReactiveBridge.fireAndForget(requestContext, () -> {
                    releaseWaitingPermitIfPresent(streamSession);
                    aiChatRequestPreparer.releaseRequestReservation(streamSession.getReservedContext());
                });
            });
        });
    }

    private Flux<CommonStreamResult<AiChatStreamEventVo>> executeReactiveStream(AiChatReactiveRequestContext requestContext,
                                                                                 ReactiveStreamSession streamSession,
                                                                                 AiChatCapacityPermit capacityPermit) {
        AtomicReference<StreamProcessResult> completionRef = new AtomicReference<>(emptyStreamProcessResult());
        return aiChatReactiveBridge.blocking(requestContext,
                        () -> prepareStreamContext(streamSession, capacityPermit))
                .flatMapMany(context -> {
                    streamSession.setPreparedContext(context);
                    return Flux.concat(
                            Flux.just(buildStartEvent(context)),
                            aiChatStreamProcessor.process(
                                    context,
                                    aiChatClient.streamChat(context.getAiChatRequest()),
                                    completionRef
                            ),
                            aiChatReactiveBridge.blockingRunnable(requestContext, () -> {
                                AiChatCompletionResult completionResult = toCompletionResult(requiredStreamProcessResult(completionRef));
                                validateCompletion(completionResult);
                                aiChatOutcomeApplier.applySuccess(context, completionResult, AiChatRequestStatusEnum.SUCCESS.name());
                                aiChatSummaryDispatcher.publishRoundSummary(context);
                            }).thenMany(Mono.fromSupplier(() ->
                                    aiChatStreamProcessor.buildEndEvent(
                                            context,
                                            requiredStreamProcessResult(completionRef)
                                    )).flux())
                    );
                })
                .onErrorResume(ex -> handlePreparedStreamError(requestContext, streamSession, completionRef, ex))
                .doFinally(signalType -> handleStreamFinally(requestContext, streamSession, completionRef, signalType));
    }

    private Flux<CommonStreamResult<AiChatStreamEventVo>> handlePreparedStreamError(AiChatReactiveRequestContext requestContext,
                                                                                     ReactiveStreamSession streamSession,
                                                                                     AtomicReference<StreamProcessResult> completionRef,
                                                                                     Throwable throwable) {
        PreparedSendContext context = streamSession.getPreparedContext();
        if (context == null) {
            return emitBootstrapError(requestContext, streamSession, throwable);
        }
        ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(toException(throwable));
        StreamProcessResult streamProcessResult = requiredStreamProcessResult(completionRef);
        AiChatCompletionResult completionResult = toCompletionResult(streamProcessResult);
        return aiChatReactiveBridge.blockingRunnable(requestContext, () ->
                        aiChatOutcomeApplier.applyFailure(
                                context,
                                completionResult,
                                serviceException.getCode(),
                                serviceException.getMessage(),
                                AiChatRequestStatusEnum.ERROR.name()
                        ))
                .thenMany(Mono.fromSupplier(() ->
                        aiChatStreamProcessor.buildErrorEvent(
                                context,
                                serviceException.getCode(),
                                serviceException.getMessage(),
                                streamProcessResult
                        )).flux());
    }

    private Flux<CommonStreamResult<AiChatStreamEventVo>> emitBootstrapError(AiChatReactiveRequestContext requestContext,
                                                                              ReactiveStreamSession streamSession,
                                                                              Throwable throwable) {
        ServiceException serviceException = throwable instanceof ServiceException serviceExceptionValue
                ? serviceExceptionValue
                : aiChatExceptionConverter.convertToServiceException(toException(throwable));
        return aiChatReactiveBridge.blockingRunnable(requestContext, () -> releaseStreamResources(streamSession))
                .thenMany(Mono.fromSupplier(() ->
                        CommonStreamResult.error(
                                "error",
                                streamSession.getReservedContext().getRequestId(),
                                serviceException.getCode(),
                                serviceException.getMessage(),
                                buildStreamErrorEvent(streamSession.getReservedContext())
                        )).flux());
    }

    private void handleStreamFinally(AiChatReactiveRequestContext requestContext,
                                     ReactiveStreamSession streamSession,
                                     AtomicReference<StreamProcessResult> completionRef,
                                     SignalType signalType) {
        if (signalType == SignalType.CANCEL) {
            aiChatReactiveBridge.fireAndForget(requestContext,
                    () -> applyCancelledAndRelease(streamSession, completionRef));
            return;
        }
        aiChatReactiveBridge.fireAndForget(requestContext,
                () -> releaseStreamResources(streamSession));
    }

    private PreparedSendContext prepareStreamContext(ReactiveStreamSession streamSession,
                                                     AiChatCapacityPermit capacityPermit) {
        ReservedSendContext reservedContext = streamSession.getReservedContext();
        streamSession.setActivePermitId(capacityPermit.getActivePermitId());
        streamSession.setWaited(capacityPermit.isWaited());

        if (!aiChatConcurrencyCoordinator.acquireConversationLock(reservedContext.getConversationId())) {
            aiChatRequestPreparer.releaseRequestReservation(reservedContext);
            throw new ServiceException(GlobalErrorCode.CHAT_GENERATING_CONFLICT);
        }
        streamSession.setConversationLockAcquired(true);

        PreparedSendContext context = transactionTemplate.execute(status -> {
            PreparedSendContext preparedSendContext = prepareSendContext(
                    reservedContext,
                    AiChatRequestModeEnum.STREAM,
                    capacityPermit
            );
            markRequestStatus(preparedSendContext, AiChatRequestStatusEnum.STREAMING.name());
            return preparedSendContext;
        });
        if (context == null) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE);
        }
        return context;
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

    private CommonStreamResult<AiChatStreamEventVo> buildWaitingEvent(ReactiveStreamSession streamSession) {
        AiChatStreamEventVo waitingEvent = aiChatAssembler.toStreamEventVo(
                streamSession.getReservedContext().getConversationId(),
                null,
                streamSession.getReservedContext().getRequestId(),
                0
        );
        waitingEvent.setContentStatus(AiContentStatusEnum.WAITING.name());
        waitingEvent.setContent("当前并发较高，已进入等待队列");
        return CommonStreamResult.success("waiting", streamSession.getReservedContext().getRequestId(), waitingEvent);
    }

    private CommonStreamResult<AiChatStreamEventVo> buildStartEvent(PreparedSendContext context) {
        AiChatStreamEventVo startEvent = aiChatAssembler.toStreamEventVo(
                context.getConversationId(),
                context.getAssistantMessage().getId(),
                context.getRequestId(),
                0
        );
        startEvent.setContentStatus(AiContentStatusEnum.GENERATING.name());
        return CommonStreamResult.success("message-start", context.getRequestId(), startEvent);
    }

    private void applyCancelledAndRelease(ReactiveStreamSession streamSession,
                                          AtomicReference<StreamProcessResult> completionRef) {
        if (!streamSession.getCleanupStarted().compareAndSet(false, true)) {
            return;
        }
        try {
            PreparedSendContext context = streamSession.getPreparedContext();
            if (context == null) {
                aiChatRequestPreparer.releaseRequestReservation(streamSession.getReservedContext());
                return;
            }
            aiChatOutcomeApplier.applyCancelled(
                    context,
                    toCompletionResult(requiredStreamProcessResult(completionRef))
            );
        } finally {
            releasePermitsAndLock(streamSession);
        }
    }

    private void releaseStreamResources(ReactiveStreamSession streamSession) {
        if (!streamSession.getCleanupStarted().compareAndSet(false, true)) {
            return;
        }
        try {
            if (streamSession.getPreparedContext() == null) {
                aiChatRequestPreparer.releaseRequestReservation(streamSession.getReservedContext());
            }
        } finally {
            releasePermitsAndLock(streamSession);
        }
    }

    private void releasePermitsAndLock(ReactiveStreamSession streamSession) {
        releaseWaitingPermitIfPresent(streamSession);
        releaseActivePermitIfPresent(streamSession);
        releaseConversationLockIfHeld(streamSession);
    }

    private void releaseWaitingPermitIfPresent(ReactiveStreamSession streamSession) {
        if (streamSession.getWaitingPermitId() != null) {
            aiChatConcurrencyCoordinator.releaseWaitingPermit(streamSession.getWaitingPermitId());
            streamSession.setWaitingPermitId(null);
        }
    }

    private void releaseActivePermitIfPresent(ReactiveStreamSession streamSession) {
        if (streamSession.getActivePermitId() != null) {
            aiChatConcurrencyCoordinator.releaseActivePermit(streamSession.getActivePermitId());
            streamSession.setActivePermitId(null);
        }
    }

    private void releaseConversationLockIfHeld(ReactiveStreamSession streamSession) {
        if (streamSession.isConversationLockAcquired()) {
            aiChatConcurrencyCoordinator.releaseConversationLock(streamSession.getReservedContext().getConversationId());
            streamSession.setConversationLockAcquired(false);
        }
    }

    private StreamProcessResult emptyStreamProcessResult() {
        return StreamProcessResult.builder()
                .completedAt(LocalDateTime.now())
                .streamBatchCount(0)
                .build();
    }

    private StreamProcessResult requiredStreamProcessResult(AtomicReference<StreamProcessResult> completionRef) {
        StreamProcessResult completionResult = completionRef.get();
        return completionResult != null ? completionResult : emptyStreamProcessResult();
    }

    private AiChatCompletionResult toCompletionResult(StreamProcessResult streamResult) {
        return AiChatCompletionResult.builder()
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
    }

    private Exception toException(Throwable throwable) {
        if (throwable instanceof Exception exception) {
            return exception;
        }
        return new RuntimeException(throwable);
    }

}
