package com.vita.aichat.chat.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.model.context.PreparedSendContext;
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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final ChatModel chatModel;
    private final AiChatProperty aiChatProperty;
    private final AiChatAssembler aiChatAssembler;
    private final AiChatRedisCoordinator aiChatRedisCoordinator;
    private final AiChatSseWriter aiChatSseWriter;
    private final AiChatResponseExtractor aiChatResponseExtractor;
    private final Executor aiChatExecutor;
    private final AiChatStreamProcessor aiChatStreamProcessor;
    private final AiChatOutcomeApplier aiChatOutcomeApplier;
    private final AiChatRequestPreparer aiChatRequestPreparer;
    private final AiChatExceptionConverter aiChatExceptionConverter;

    public AiChatServiceImpl(IAiConversationService aiConversationService,
                             IAiMessageService aiMessageService,
                             IAiConversationSummaryService aiConversationSummaryService,
                             IAiChatRequestService aiChatRequestService,
                             ChatModel chatModel,
                             AiChatProperty aiChatProperty,
                             AiChatAssembler aiChatAssembler,
                             AiChatRedisCoordinator aiChatRedisCoordinator,
                             AiChatSseWriter aiChatSseWriter,
                             AiChatResponseExtractor aiChatResponseExtractor,
                             @Qualifier("aiChatExecutor") Executor aiChatExecutor,
                             AiChatStreamProcessor aiChatStreamProcessor,
                             AiChatOutcomeApplier aiChatOutcomeApplier,
                             AiChatRequestPreparer aiChatRequestPreparer,
                             AiChatExceptionConverter aiChatExceptionConverter) {
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiChatRequestService = aiChatRequestService;
        this.chatModel = chatModel;
        this.aiChatProperty = aiChatProperty;
        this.aiChatAssembler = aiChatAssembler;
        this.aiChatRedisCoordinator = aiChatRedisCoordinator;
        this.aiChatSseWriter = aiChatSseWriter;
        this.aiChatResponseExtractor = aiChatResponseExtractor;
        this.aiChatExecutor = aiChatExecutor;
        this.aiChatStreamProcessor = aiChatStreamProcessor;
        this.aiChatOutcomeApplier = aiChatOutcomeApplier;
        this.aiChatRequestPreparer = aiChatRequestPreparer;
        this.aiChatExceptionConverter = aiChatExceptionConverter;
    }

    /**
     * 创建会话。
     *
     * @param createDto 创建请求对象
     * @return 会话ID
     */
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
        conversation.setCurrentModelCode(aiChatRequestPreparer.generateModelCode(createDto.getModelCode()));
        conversation.setSystemPrompt(aiChatRequestPreparer.generateSystemPrompt(createDto.getSystemPrompt()));
        conversation.setTemperature(createDto.getTemperature());
        conversation.setMaxTokens(createDto.getMaxTokens());
        conversation.setMaxContextMessages(aiChatRequestPreparer.generateMaxContextMessages(createDto.getMaxContextMessages()));
        aiConversationService.create(conversation);
        return conversation.getId();
    }

    /**
     * 分页查询会话列表。
     *
     * @param queryDto 分页查询条件
     * @return 会话分页数据
     */
    @Override
    public PageResponse<AiConversationPageVo> pageConversations(AiConversationPageQueryDto queryDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PageResponse<AiConversation> pageResponse = aiConversationService.pageByUserId(queryDto, userId, queryDto.getKeyword());
        PageResponse<AiConversationPageVo> response = new PageResponse<>();
        response.setTotal(pageResponse.getTotal());
        response.setList(pageResponse.getList().stream().map(aiChatAssembler::toConversationPageVo).toList());
        return response;
    }

    /**
     * 查询会话详情。
     *
     * @param conversationId 会话ID
     * @return 会话详情对象
     */
    @Override
    public AiConversationDetailVo getConversation(Long conversationId) {
        return aiChatAssembler.toConversationDetailVo(aiChatRequestPreparer.requireConversation(conversationId));
    }

    /**
     * 重命名会话标题。
     *
     * @param conversationId 会话ID
     * @param updateDto      标题更新请求对象
     */
    @Override
    public void renameConversation(Long conversationId, AiConversationTitleUpdateDto updateDto) {
        AiConversation conversation = aiChatRequestPreparer.requireConversation(conversationId);
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setTitle(updateDto.getTitle().trim());
        update.setTitleSource(AiTitleSourceEnum.MANUAL.name());
        aiConversationService.update(update);
    }

    /**
     * 删除会话及其关联数据。
     *
     * @param conversationId 会话ID
     */
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

    /**
     * 分页查询消息列表。
     *
     * @param conversationId 会话ID
     * @param queryDto       分页查询条件
     * @return 消息分页数据
     */
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

    /**
     * 发送消息（同步模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @return 发送结果
     */
    @Override
    public AiChatSendVo sendMessage(Long conversationId, AiMessageSendDto sendDto) {
        PreparedSendContext context = prepareSendContext(conversationId, sendDto, AiChatRequestModeEnum.SYNC);
        try {
            ChatResponse response = chatModel.call(context.getPrompt());
            AiChatCompletionResult completionResult = aiChatResponseExtractor.extractCompletion(response);
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
            return aiChatAssembler.toSendVo(context.getUserMessage(), context.getAssistantMessage(), completionResult);
        } catch (Exception ex) {
            ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(ex);
            aiChatOutcomeApplier.applyFailure(context, emptyCompletionResult(), serviceException.getCode(), serviceException.getMessage(), AiChatRequestStatusEnum.ERROR.name());
            throw serviceException;
        } finally {
            aiChatRedisCoordinator.releaseConversationLock(conversationId);
        }
    }

    /**
     * 发送消息（流式模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @return SSE流式响应
     */
    @Override
    public SseEmitter sendMessageStream(Long conversationId, AiMessageSendDto sendDto) {
        if (!Boolean.TRUE.equals(aiChatProperty.getStreamEnabled())) {
            throw new ServiceException(GlobalErrorCode.CHAT_STREAM_DISABLED);
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
            // 在提交异步任务时传递上下文信息，确保在异步线程中也能获取到登录用户信息等上下文数据
            LoginUserInfoModel currentUserInfo = LoginUserInfoModelContext.getLoginUserInfo();
            aiChatExecutor.execute(() -> {
                try {
                    LoginUserInfoModelContext.setLoginUserInfo(currentUserInfo);
                    processStreamWithContext(context, emitter);
                } finally {
                    LoginUserInfoModelContext.removeLoginUserInfo();
                }
            });
        } catch (RejectedExecutionException ex) {
            ServiceException serviceException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE);
            aiChatOutcomeApplier.applyFailure(context, emptyCompletionResult(), serviceException.getCode(), serviceException.getMessage(), AiChatRequestStatusEnum.ERROR.name());
            sendStreamErrorQuietly(emitter, context.getRequestId(), serviceException.getCode(), serviceException.getMessage(), startEvent);
            emitter.complete();
            aiChatRedisCoordinator.releaseConversationLock(conversationId);
        }
        return emitter;
    }

    /**
     * 处理流式响应（带上下文）。
     *
     * @param context 发送准备上下文
     * @param emitter SSE 发射器
     */
    private void processStreamWithContext(PreparedSendContext context, SseEmitter emitter) {
        markRequestStatus(context, AiChatRequestStatusEnum.STREAMING.name());

        try {
            // 委托给流式处理器
            StreamProcessResult streamResult = aiChatStreamProcessor.process(context, emitter);

            // 转换为完成结果
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

            // 应用成功结果
            aiChatOutcomeApplier.applySuccess(context, completionResult, AiChatRequestStatusEnum.SUCCESS.name());

            // 发送结束事件
            aiChatStreamProcessor.sendEndEvent(context, emitter, streamResult);
            emitter.complete();
        } catch (Exception ex) {
            ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(ex);

            // 提取流式处理结果（如果有的话）
            AiChatCompletionResult completionResult = extractCompletionFromException(ex);

            // 应用失败结果
            aiChatOutcomeApplier.applyFailure(context, completionResult, serviceException.getCode(), serviceException.getMessage(), AiChatRequestStatusEnum.ERROR.name());

            // 发送错误事件
            aiChatStreamProcessor.sendErrorEvent(context, emitter, serviceException.getCode(), serviceException.getMessage(), null);
            emitter.complete();
        } finally {
            aiChatRedisCoordinator.releaseConversationLock(context.getConversationId());
        }
    }

    /**
     * 从异常中提取完成结果。
     *
     * @param ex 异常
     * @return 完成结果
     */
    private AiChatCompletionResult extractCompletionFromException(Exception ex) {
        return emptyCompletionResult();
    }

    /**
     * 准备发送消息上下文。
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @param requestMode    请求模式
     * @return 发送准备上下文
     */
    private PreparedSendContext prepareSendContext(Long conversationId,
                                                   AiMessageSendDto sendDto,
                                                   AiChatRequestModeEnum requestMode) {
        return aiChatRequestPreparer.prepareSendContext(conversationId, sendDto, requestMode);
    }

    /**
     * 标记请求状态。
     *
     * @param context 发送准备上下文
     * @param status  请求状态
     */
    private void markRequestStatus(PreparedSendContext context, String status) {
        AiChatRequest update = new AiChatRequest();
        update.setId(context.getChatRequest().getId());
        update.setStatus(status);
        aiChatRequestService.update(update);
    }

    /**
     * 校验模型完成结果。
     *
     * @param completionResult 模型完成结果
     */
    private void validateCompletion(AiChatCompletionResult completionResult) {
        if (completionResult == null) {
            throw new ServiceException(GlobalErrorCode.MODEL_RESPONSE_INVALID);
        }
        if (CharSequenceUtil.isBlank(completionResult.getContent())
                && CharSequenceUtil.isBlank(completionResult.getReasoningContent())) {
            throw new ServiceException(GlobalErrorCode.MODEL_RESPONSE_INVALID);
        }
    }

    /**
     * 构建空完成结果。
     *
     * @return 空完成结果
     */
    private AiChatCompletionResult emptyCompletionResult() {
        return AiChatCompletionResult.builder()
                .completedAt(LocalDateTime.now())
                .streamBatchCount(0)
                .build();
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
            LOG.warn("SSE success event send failed, event={}, requestId={}", event, requestId, ioException);
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
            LOG.warn("SSE error event send failed, requestId={}", requestId, ioException);
        }
    }
}
