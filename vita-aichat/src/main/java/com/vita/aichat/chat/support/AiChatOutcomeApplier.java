package com.vita.aichat.chat.support;

import com.vita.aichat.aiChatRequest.entity.AiChatRequest;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.enums.AiChatRequestStatusEnum;
import com.vita.aichat.enums.AiContentStatusEnum;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * AI Chat 结果应用器。
 *
 * <p>负责将模型调用的成功或失败结果应用到数据库中的相关实体。</p>
 */
@Component
public class AiChatOutcomeApplier {

    private final IAiMessageService aiMessageService;

    private final IAiChatRequestService aiChatRequestService;

    private final IAiConversationService aiConversationService;

    private final TransactionTemplate transactionTemplate;

    public AiChatOutcomeApplier(IAiMessageService aiMessageService,
                                IAiChatRequestService aiChatRequestService,
                                IAiConversationService aiConversationService,
                                TransactionTemplate transactionTemplate) {
        this.aiMessageService = aiMessageService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiConversationService = aiConversationService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 应用成功结果到数据库。
     *
     * @param context          发送准备上下文
     * @param completionResult 模型完成结果
     * @param requestStatus    请求状态
     */
    public void applySuccess(PreparedSendContext context,
                             AiChatCompletionResult completionResult,
                             String requestStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            updateAssistantMessageForSuccess(context, completionResult);
            updateChatRequestForSuccess(context, completionResult, requestStatus);
            updateConversationForSuccess(context, completionResult);
            context.getAssistantMessage().setContentStatus(AiContentStatusEnum.DONE.name());
        });
    }

    /**
     * 应用失败结果到数据库。
     *
     * @param context          发送准备上下文
     * @param completionResult 模型完成结果
     * @param errorCode        错误码
     * @param errorMessage     错误信息
     * @param requestStatus    请求状态
     */
    public void applyFailure(PreparedSendContext context,
                             AiChatCompletionResult completionResult,
                             Integer errorCode,
                             String errorMessage,
                             String requestStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            updateAssistantMessageForFailure(context, completionResult, errorCode, errorMessage);
            updateChatRequestForFailure(context, completionResult, errorCode, errorMessage, requestStatus);
            updateConversationForFailure(context, errorCode, errorMessage);
            context.getAssistantMessage().setContentStatus(AiContentStatusEnum.ERROR.name());
        });
    }

    /**
     * 应用取消结果到数据库。
     *
     * <p>客户端主动断开连接时，回写已生成的部分内容并清理会话活跃请求状态。</p>
     */
    public void applyCancelled(PreparedSendContext context,
                               AiChatCompletionResult completionResult) {
        transactionTemplate.executeWithoutResult(status -> {
            AiMessage assistantUpdate = new AiMessage();
            assistantUpdate.setId(context.getAssistantMessage().getId());
            assistantUpdate.setProviderRequestId(completionResult.getProviderRequestId());
            assistantUpdate.setContent(completionResult.getContent());
            assistantUpdate.setReasoningContent(completionResult.getReasoningContent());
            assistantUpdate.setContentStatus(AiContentStatusEnum.CANCELLED.name());
            assistantUpdate.setFinishReason(completionResult.getFinishReason());
            assistantUpdate.setTokenInput(completionResult.getTokenInput());
            assistantUpdate.setTokenOutput(completionResult.getTokenOutput());
            assistantUpdate.setTokenTotal(completionResult.getTokenTotal());
            assistantUpdate.setFirstTokenAt(completionResult.getFirstTokenAt());
            assistantUpdate.setCompletedAt(completionResult.getCompletedAt() != null ? completionResult.getCompletedAt() : LocalDateTime.now());
            assistantUpdate.setStreamBatchCount(defaultBatchCount(completionResult.getStreamBatchCount()));
            assistantUpdate.setErrorCode(null);
            assistantUpdate.setErrorMessage(null);
            aiMessageService.update(assistantUpdate);

            AiChatRequest requestUpdate = new AiChatRequest();
            requestUpdate.setId(context.getChatRequest().getId());
            requestUpdate.setProviderRequestId(completionResult.getProviderRequestId());
            requestUpdate.setStatus(AiChatRequestStatusEnum.CANCELLED.name());
            requestUpdate.setFinishReason(completionResult.getFinishReason());
            requestUpdate.setTokenInput(completionResult.getTokenInput());
            requestUpdate.setTokenOutput(completionResult.getTokenOutput());
            requestUpdate.setTokenTotal(completionResult.getTokenTotal());
            requestUpdate.setFirstTokenAt(completionResult.getFirstTokenAt());
            requestUpdate.setCompletedAt(completionResult.getCompletedAt() != null ? completionResult.getCompletedAt() : LocalDateTime.now());
            requestUpdate.setErrorCode(null);
            requestUpdate.setErrorMessage(null);
            aiChatRequestService.update(requestUpdate);

            AiConversation conversationUpdate = new AiConversation();
            conversationUpdate.setId(context.getConversationId());
            conversationUpdate.setActiveRequestId(null);
            conversationUpdate.setLastErrorCode(null);
            conversationUpdate.setLastErrorMessage(null);
            aiConversationService.update(conversationUpdate);
            context.getAssistantMessage().setContentStatus(AiContentStatusEnum.CANCELLED.name());
        });
    }

    /**
     * 更新助手消息为成功状态。
     */
    private void updateAssistantMessageForSuccess(PreparedSendContext context,
                                                   AiChatCompletionResult completionResult) {
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
    }

    /**
     * 更新聊天请求为成功状态。
     */
    private void updateChatRequestForSuccess(PreparedSendContext context,
                                              AiChatCompletionResult completionResult,
                                              String requestStatus) {
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
    }

    /**
     * 更新会话为成功状态。
     */
    private void updateConversationForSuccess(PreparedSendContext context,
                                               AiChatCompletionResult completionResult) {
        AiConversation conversationUpdate = new AiConversation();
        conversationUpdate.setId(context.getConversationId());
        conversationUpdate.setActiveRequestId(null);
        conversationUpdate.setLastErrorCode(null);
        conversationUpdate.setLastErrorMessage(null);
        conversationUpdate.setLastMessageAt(completionResult.getCompletedAt());
        aiConversationService.update(conversationUpdate);
    }

    /**
     * 更新助手消息为失败状态。
     */
    private void updateAssistantMessageForFailure(PreparedSendContext context,
                                                   AiChatCompletionResult completionResult,
                                                   Integer errorCode,
                                                   String errorMessage) {
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
        assistantUpdate.setErrorCode(errorCode != null ? String.valueOf(errorCode) : null);
        assistantUpdate.setErrorMessage(errorMessage);
        aiMessageService.update(assistantUpdate);
    }

    /**
     * 更新聊天请求为失败状态。
     */
    private void updateChatRequestForFailure(PreparedSendContext context,
                                              AiChatCompletionResult completionResult,
                                              Integer errorCode,
                                              String errorMessage,
                                              String requestStatus) {
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
        requestUpdate.setErrorCode(errorCode != null ? String.valueOf(errorCode) : null);
        requestUpdate.setErrorMessage(errorMessage);
        aiChatRequestService.update(requestUpdate);
    }

    /**
     * 更新会话为失败状态。
     */
    private void updateConversationForFailure(PreparedSendContext context,
                                               Integer errorCode,
                                               String errorMessage) {
        AiConversation conversationUpdate = new AiConversation();
        conversationUpdate.setId(context.getConversationId());
        conversationUpdate.setActiveRequestId(null);
        conversationUpdate.setLastErrorCode(errorCode != null ? String.valueOf(errorCode) : null);
        conversationUpdate.setLastErrorMessage(errorMessage);
        aiConversationService.update(conversationUpdate);
    }

    /**
     * 获取默认批次数量。
     *
     * @param value 批次数量
     * @return 默认批次数量
     */
    private Integer defaultBatchCount(Integer value) {
        return value == null ? 0 : value;
    }
}
