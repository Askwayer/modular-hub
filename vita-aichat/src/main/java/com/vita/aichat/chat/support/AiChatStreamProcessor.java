package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.model.AiStreamChunk;
import com.vita.aichat.aiMessageStreamBatch.entity.AiMessageStreamBatch;
import com.vita.aichat.aiMessageStreamBatch.service.IAiMessageStreamBatchService;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.result.AiChatStreamBatchSnapshot;
import com.vita.aichat.chat.model.result.StreamProcessResult;
import com.vita.aichat.chat.vo.AiChatStreamEventVo;
import com.vita.aichat.enums.AiContentStatusEnum;
import com.vita.aichat.enums.AiStreamFlushReasonEnum;
import com.vita.core.CommonStreamResult;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.utils.SnowUtils;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式响应处理器。
 */
@Component
public class AiChatStreamProcessor {

    private static final long STREAM_BATCH_FLUSH_MILLIS = 200L;
    private static final int STREAM_BATCH_CHAR_THRESHOLD = 64;

    private final AiChatAssembler aiChatAssembler;
    private final IAiMessageStreamBatchService aiMessageStreamBatchService;
    private final AiChatReactiveBridge aiChatReactiveBridge;

    public AiChatStreamProcessor(AiChatAssembler aiChatAssembler,
                                 IAiMessageStreamBatchService aiMessageStreamBatchService,
                                 AiChatReactiveBridge aiChatReactiveBridge) {
        this.aiChatAssembler = aiChatAssembler;
        this.aiMessageStreamBatchService = aiMessageStreamBatchService;
        this.aiChatReactiveBridge = aiChatReactiveBridge;
    }

    /**
     * 执行流式响应处理，返回 NDJSON 事件流。
     */
    public Flux<CommonStreamResult<AiChatStreamEventVo>> process(PreparedSendContext context,
                                                                 Flux<AiStreamChunk> streamFlux,
                                                                 AtomicReference<StreamProcessResult> completionRef) {
        Scheduler scheduler = aiChatReactiveBridge.blockingScheduler();
        return Flux.create(sink -> {
            StreamState state = new StreamState();
            Scheduler.Worker worker = scheduler.createWorker();
            AtomicBoolean cancelled = new AtomicBoolean(false);
            AtomicReference<Disposable> upstreamRef = new AtomicReference<>();

            Disposable periodicFlush = worker.schedulePeriodically(
                    () -> flushByTimeWindow(context, state, sink),
                    STREAM_BATCH_FLUSH_MILLIS,
                    STREAM_BATCH_FLUSH_MILLIS,
                    TimeUnit.MILLISECONDS
            );

            Disposable upstream = streamFlux.subscribe(
                    chunk -> worker.schedule(() -> handleChunk(context, state, chunk, sink)),
                    throwable -> worker.schedule(() -> handleError(context, state, sink, completionRef, throwable, periodicFlush, worker)),
                    () -> worker.schedule(() -> handleComplete(context, state, sink, completionRef, periodicFlush, worker))
            );
            upstreamRef.set(upstream);

            sink.onCancel(() -> {
                cancelled.set(true);
                Disposable upstreamDisposable = upstreamRef.get();
                if (upstreamDisposable != null) {
                    upstreamDisposable.dispose();
                }
                worker.schedule(() -> handleCancel(context, state, completionRef, periodicFlush, worker));
            });

            sink.onDispose(() -> {
                if (!cancelled.get()) {
                    periodicFlush.dispose();
                    worker.dispose();
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    public CommonStreamResult<AiChatStreamEventVo> buildEndEvent(PreparedSendContext context,
                                                                 StreamProcessResult completionResult) {
        AiChatStreamEventVo endEvent = aiChatAssembler.toStreamEventVo(
                context.getConversationId(),
                context.getAssistantMessage().getId(),
                context.getRequestId(),
                completionResult.getStreamBatchCount()
        );
        endEvent.setFinishReason(completionResult.getFinishReason());
        endEvent.setContentStatus(AiContentStatusEnum.DONE.name());
        endEvent.setFirstTokenAt(completionResult.getFirstTokenAt());
        endEvent.setCompletedAt(completionResult.getCompletedAt());
        endEvent.setTokenInput(completionResult.getTokenInput());
        endEvent.setTokenOutput(completionResult.getTokenOutput());
        endEvent.setTokenTotal(completionResult.getTokenTotal());
        return CommonStreamResult.success("message-end", context.getRequestId(), endEvent);
    }

    public CommonStreamResult<AiChatStreamEventVo> buildErrorEvent(PreparedSendContext context,
                                                                   Integer errorCode,
                                                                   String errorMessage,
                                                                   StreamProcessResult completionResult) {
        AiChatStreamEventVo errorEvent = aiChatAssembler.toStreamEventVo(
                context.getConversationId(),
                context.getAssistantMessage().getId(),
                context.getRequestId(),
                completionResult != null ? completionResult.getStreamBatchCount() : 0
        );
        errorEvent.setContentStatus(AiContentStatusEnum.ERROR.name());
        errorEvent.setFinishReason(completionResult != null ? completionResult.getFinishReason() : null);
        return CommonStreamResult.error("error", context.getRequestId(), errorCode, errorMessage, errorEvent);
    }

    private void handleChunk(PreparedSendContext context,
                             StreamState state,
                             AiStreamChunk chunk,
                             FluxSink<CommonStreamResult<AiChatStreamEventVo>> sink) {
        if (state.terminated.get()) {
            return;
        }
        state.providerSequence++;
        if (CharSequenceUtil.isNotBlank(chunk.getProviderRequestId())) {
            state.providerRequestId = chunk.getProviderRequestId();
        }
        if (chunk.getTokenInput() != null) {
            state.tokenInput = chunk.getTokenInput();
        }
        if (chunk.getTokenOutput() != null) {
            state.tokenOutput = chunk.getTokenOutput();
        }
        if (chunk.getTokenTotal() != null) {
            state.tokenTotal = chunk.getTokenTotal();
        }
        if (CharSequenceUtil.isNotBlank(chunk.getFinishReason())) {
            state.finishReason = chunk.getFinishReason();
        }
        if (state.firstTokenAt == null && hasDelta(chunk)) {
            state.firstTokenAt = LocalDateTime.now();
        }
        if (CharSequenceUtil.isNotBlank(chunk.getDelta())) {
            state.fullContent.append(chunk.getDelta());
        }
        if (CharSequenceUtil.isNotBlank(chunk.getReasoningDelta())) {
            state.fullReasoning.append(chunk.getReasoningDelta());
        }
        state.batchAccumulator.append(chunk.getDelta(), chunk.getReasoningDelta());
        if (state.batchAccumulator.reachedCharThreshold()) {
            flushBatch(context, state, sink, AiStreamFlushReasonEnum.CHAR_THRESHOLD);
        }
    }

    private void handleComplete(PreparedSendContext context,
                                StreamState state,
                                FluxSink<CommonStreamResult<AiChatStreamEventVo>> sink,
                                AtomicReference<StreamProcessResult> completionRef,
                                Disposable periodicFlush,
                                Scheduler.Worker worker) {
        if (!state.terminated.compareAndSet(false, true)) {
            return;
        }
        periodicFlush.dispose();
        try {
            if (!state.batchAccumulator.hasPending()
                    && state.fullContent.length() == 0
                    && state.fullReasoning.length() == 0) {
                throw new ServiceException(GlobalErrorCode.MODEL_RESPONSE_INVALID);
            }
            if (state.batchAccumulator.hasPending()) {
                flushBatch(context, state, sink, AiStreamFlushReasonEnum.FINAL);
            }
            completionRef.set(state.snapshot(LocalDateTime.now()));
            sink.complete();
        } catch (Exception ex) {
            sink.error(ex);
        } finally {
            worker.dispose();
        }
    }

    private void handleError(PreparedSendContext context,
                             StreamState state,
                             FluxSink<CommonStreamResult<AiChatStreamEventVo>> sink,
                             AtomicReference<StreamProcessResult> completionRef,
                             Throwable throwable,
                             Disposable periodicFlush,
                             Scheduler.Worker worker) {
        if (!state.terminated.compareAndSet(false, true)) {
            return;
        }
        periodicFlush.dispose();
        try {
            if (state.batchAccumulator.hasPending()) {
                flushBatch(context, state, sink, AiStreamFlushReasonEnum.ERROR);
            }
            completionRef.set(state.snapshot(LocalDateTime.now()));
            sink.error(throwable);
        } finally {
            worker.dispose();
        }
    }

    private void handleCancel(PreparedSendContext context,
                              StreamState state,
                              AtomicReference<StreamProcessResult> completionRef,
                              Disposable periodicFlush,
                              Scheduler.Worker worker) {
        if (!state.terminated.compareAndSet(false, true)) {
            return;
        }
        periodicFlush.dispose();
        try {
            if (state.batchAccumulator.hasPending()) {
                flushBatch(context, state, null, AiStreamFlushReasonEnum.CANCELLED);
            }
            completionRef.set(state.snapshot(LocalDateTime.now()));
        } finally {
            worker.dispose();
        }
    }

    private void flushByTimeWindow(PreparedSendContext context,
                                   StreamState state,
                                   FluxSink<CommonStreamResult<AiChatStreamEventVo>> sink) {
        if (state.terminated.get()) {
            return;
        }
        if (state.batchAccumulator.shouldFlush(System.currentTimeMillis()) && !state.batchAccumulator.reachedCharThreshold()) {
            flushBatch(context, state, sink, AiStreamFlushReasonEnum.TIME_WINDOW);
        }
    }

    private int flushBatch(PreparedSendContext context,
                           StreamState state,
                           FluxSink<CommonStreamResult<AiChatStreamEventVo>> sink,
                           AiStreamFlushReasonEnum flushReason) {
        if (!state.batchAccumulator.hasPending()) {
            return state.batchNo;
        }
        int nextBatchNo = state.batchNo + 1;
        AiChatStreamBatchSnapshot snapshot = state.batchAccumulator.drain(System.currentTimeMillis());

        AiMessageStreamBatch batch = new AiMessageStreamBatch();
        batch.setId(SnowUtils.getSnowflakeNextId());
        batch.setAssistantMessageId(context.getAssistantMessage().getId());
        batch.setRequestId(context.getRequestId());
        batch.setBatchNo(nextBatchNo);
        batch.setContentDelta(snapshot.getContentDelta());
        batch.setReasoningDelta(snapshot.getReasoningDelta());
        batch.setFlushReason(flushReason.name());
        batch.setDeltaChars(snapshot.getDeltaChars());
        batch.setProviderSequence(state.providerSequence);
        aiMessageStreamBatchService.create(batch);

        if (sink != null && CharSequenceUtil.isNotBlank(snapshot.getContentDelta())) {
            AiChatStreamEventVo deltaEvent = aiChatAssembler.toStreamEventVo(
                    context.getConversationId(),
                    context.getAssistantMessage().getId(),
                    context.getRequestId(),
                    nextBatchNo
            );
            deltaEvent.setContent(snapshot.getContentDelta());
            sink.next(CommonStreamResult.success("message-delta", context.getRequestId(), deltaEvent));
        }

        if (sink != null && CharSequenceUtil.isNotBlank(snapshot.getReasoningDelta())) {
            AiChatStreamEventVo reasoningEvent = aiChatAssembler.toStreamEventVo(
                    context.getConversationId(),
                    context.getAssistantMessage().getId(),
                    context.getRequestId(),
                    nextBatchNo
            );
            reasoningEvent.setReasoningContent(snapshot.getReasoningDelta());
            sink.next(CommonStreamResult.success("reasoning-delta", context.getRequestId(), reasoningEvent));
        }

        state.batchNo = nextBatchNo;
        return nextBatchNo;
    }

    private boolean hasDelta(AiStreamChunk chunk) {
        return chunk != null && (CharSequenceUtil.isNotBlank(chunk.getDelta())
                || CharSequenceUtil.isNotBlank(chunk.getReasoningDelta()));
    }

    private static class StreamState {

        private final AiStreamBatchAccumulator batchAccumulator = new AiStreamBatchAccumulator(
                STREAM_BATCH_FLUSH_MILLIS,
                STREAM_BATCH_CHAR_THRESHOLD,
                System.currentTimeMillis()
        );
        private final StringBuilder fullContent = new StringBuilder();
        private final StringBuilder fullReasoning = new StringBuilder();
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        private LocalDateTime firstTokenAt;
        private String providerRequestId;
        private String finishReason;
        private Integer tokenInput;
        private Integer tokenOutput;
        private Integer tokenTotal;
        private int providerSequence;
        private int batchNo;

        private StreamProcessResult snapshot(LocalDateTime completedAt) {
            return StreamProcessResult.builder()
                    .providerRequestId(providerRequestId)
                    .content(fullContent.toString())
                    .reasoningContent(fullReasoning.toString())
                    .finishReason(finishReason)
                    .tokenInput(tokenInput)
                    .tokenOutput(tokenOutput)
                    .tokenTotal(tokenTotal)
                    .firstTokenAt(firstTokenAt != null ? firstTokenAt : completedAt)
                    .completedAt(completedAt)
                    .streamBatchCount(batchNo)
                    .build();
        }
    }
}
