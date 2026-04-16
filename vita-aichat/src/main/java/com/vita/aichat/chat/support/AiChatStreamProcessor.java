package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.aichat.aiMessageStreamBatch.entity.AiMessageStreamBatch;
import com.vita.aichat.aiMessageStreamBatch.service.IAiMessageStreamBatchService;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.result.AiChatResponseChunk;
import com.vita.aichat.chat.model.result.AiChatStreamBatchSnapshot;
import com.vita.aichat.chat.model.result.StreamProcessResult;
import com.vita.aichat.chat.vo.AiChatStreamEventVo;
import com.vita.aichat.enums.AiContentStatusEnum;
import com.vita.aichat.enums.AiStreamFlushReasonEnum;
import com.vita.utils.SnowUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 流式响应处理器。
 *
 * <p>负责处理模型流式响应，包括批次累积、刷新、SSE 事件推送等。</p>
 */
@Component
public class AiChatStreamProcessor {

    /**
     * 批次刷新时间窗口（毫秒）。
     *
     * <p>控制 SSE 事件推送频率：值越小，前端响应越实时，但 SSE 事件数量越多。
     * 当前设置为 200ms，在实时性和网络开销之间取得平衡。</p>
     */
    private static final long STREAM_BATCH_FLUSH_MILLIS = 200L;

    /**
     * 批次刷新字符阈值。
     *
     * <p>控制 SSE 事件推送频率：累积到此字符数时立即推送，无需等待时间窗口。
     * 当前设置为 64 字符，确保较短文本也能快速推送到前端。</p>
     */
    private static final int STREAM_BATCH_CHAR_THRESHOLD = 64;

    private final ChatModel chatModel;

    private final AiChatResponseExtractor aiChatResponseExtractor;

    private final AiChatAssembler aiChatAssembler;

    private final IAiMessageStreamBatchService aiMessageStreamBatchService;

    private final AiChatSseWriter aiChatSseWriter;

    public AiChatStreamProcessor(ChatModel chatModel,
                                 AiChatResponseExtractor aiChatResponseExtractor,
                                 AiChatAssembler aiChatAssembler,
                                 IAiMessageStreamBatchService aiMessageStreamBatchService,
                                 AiChatSseWriter aiChatSseWriter) {
        this.chatModel = chatModel;
        this.aiChatResponseExtractor = aiChatResponseExtractor;
        this.aiChatAssembler = aiChatAssembler;
        this.aiMessageStreamBatchService = aiMessageStreamBatchService;
        this.aiChatSseWriter = aiChatSseWriter;
    }

    /**
     * 执行流式响应处理。
     *
     * @param context 发送准备上下文
     * @param emitter SSE 发射器
     * @return 流式处理结果
     */
    public StreamProcessResult process(PreparedSendContext context, SseEmitter emitter) {
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
                throw new IllegalStateException("MODEL_RESPONSE_INVALID");
            }

            if (batchAccumulator.hasPending()) {
                batchNo = flushBatch(context, emitter, batchAccumulator, batchNo, providerSequence, AiStreamFlushReasonEnum.FINAL);
            }

            return StreamProcessResult.builder()
                    .providerRequestId(providerRequestId)
                    .content(fullContent.toString())
                    .reasoningContent(fullReasoning.toString())
                    .finishReason(finishReason)
                    .tokenInput(tokenInput)
                    .tokenOutput(tokenOutput)
                    .tokenTotal(tokenTotal)
                    .firstTokenAt(firstTokenAt != null ? firstTokenAt : LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .streamBatchCount(batchNo)
                    .build();
        } catch (Exception ex) {
            if (batchAccumulator.hasPending()) {
                flushBatch(context, emitter, batchAccumulator, batchNo, providerSequence, AiStreamFlushReasonEnum.ERROR);
            }
            throw ex;
        }
    }

    /**
     * 发送流式结束事件。
     *
     * @param context          发送准备上下文
     * @param emitter          SSE 发射器
     * @param completionResult 完成结果
     */
    public void sendEndEvent(PreparedSendContext context,
                             SseEmitter emitter,
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
        sendStreamSuccessQuietly(emitter, "message-end", context.getRequestId(), endEvent);
    }

    /**
     * 发送流式错误事件。
     *
     * @param context          发送准备上下文
     * @param emitter          SSE 发射器
     * @param errorCode        错误码
     * @param errorMessage     错误信息
     * @param completionResult 完成结果
     */
    public void sendErrorEvent(PreparedSendContext context,
                               SseEmitter emitter,
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
        sendStreamErrorQuietly(emitter, context.getRequestId(), errorCode, errorMessage, errorEvent);
    }

    /**
     * 刷新批次数据。
     *
     * @param context          发送准备上下文
     * @param emitter          SSE 发射器
     * @param batchAccumulator 批次聚合器
     * @param currentBatchNo   当前批次号
     * @param providerSequence 模型提供方序列号
     * @param flushReason      刷新原因
     * @return 新批次号
     */
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

    /**
     * 判断是否有内容增量。
     *
     * @param chunk 响应片段
     * @return 是否有增量
     */
    private boolean hasDelta(AiChatResponseChunk chunk) {
        return chunk != null && (CharSequenceUtil.isNotBlank(chunk.getContentDelta())
                || CharSequenceUtil.isNotBlank(chunk.getReasoningDelta()));
    }

    /**
     * 静默发送成功事件。
     *
     * @param emitter   SSE 发射器
     * @param event     事件名称
     * @param requestId 请求唯一标识
     * @param content   事件内容
     */
    private void sendStreamSuccessQuietly(SseEmitter emitter, String event, String requestId, Object content) {
        try {
            aiChatSseWriter.sendSuccess(emitter, event, requestId, content);
        } catch (IOException ioException) {
            // SSE 事件发送失败不影响主流程
        }
    }

    /**
     * 静默发送错误事件。
     *
     * @param emitter   SSE 发射器
     * @param requestId 请求唯一标识
     * @param code      错误码
     * @param message   错误信息
     * @param content   附加内容
     */
    private void sendStreamErrorQuietly(SseEmitter emitter,
                                        String requestId,
                                        Integer code,
                                        String message,
                                        Object content) {
        try {
            aiChatSseWriter.sendError(emitter, requestId, code, message, content);
        } catch (IOException ioException) {
            // SSE 事件发送失败不影响主流程
        }
    }

}
