package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.ai.core.client.AiChatClient;
import com.vita.ai.core.model.AiChatMessage;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.enums.AiChatRequestModeEnum;
import com.vita.aichat.enums.AiChatRequestSceneEnum;
import com.vita.aichat.enums.AiChatRequestStatusEnum;
import com.vita.aichat.enums.AiSummaryStatusEnum;
import com.vita.core.exception.ServiceException;
import com.vita.utils.SnowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 多条轮次摘要再次压缩为会话级摘要。
 */
@Service
public class ConversationSummaryCompressService {

    private final IAiConversationSummaryService aiConversationSummaryService;
    private final IAiConversationService aiConversationService;
    private final IAiChatRequestService aiChatRequestService;
    private final AiChatClient aiChatClient;
    private final AiChatProperty aiChatProperty;
    private final AiChatExceptionConverter aiChatExceptionConverter;
    private final TransactionTemplate transactionTemplate;

    public ConversationSummaryCompressService(IAiConversationSummaryService aiConversationSummaryService,
                                              IAiConversationService aiConversationService,
                                              IAiChatRequestService aiChatRequestService,
                                              AiChatClient aiChatClient,
                                              AiChatProperty aiChatProperty,
                                              AiChatExceptionConverter aiChatExceptionConverter,
                                              TransactionTemplate transactionTemplate) {
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiConversationService = aiConversationService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiChatClient = aiChatClient;
        this.aiChatProperty = aiChatProperty;
        this.aiChatExceptionConverter = aiChatExceptionConverter;
        this.transactionTemplate = transactionTemplate;
    }

    public void compressIfNeeded(AiConversation conversation,
                                 Long userId,
                                 List<AiMessage> pendingSummaryMessages,
                                 String fallbackProviderCode,
                                 String fallbackModelCode) {
        if (!Boolean.TRUE.equals(aiChatProperty.getSummary().getCompressEnabled())) {
            return;
        }
        if (pendingSummaryMessages == null
                || pendingSummaryMessages.size() < aiChatProperty.getSummary().getCompressThresholdCount()) {
            return;
        }

        String requestId = IdUtil.fastSimpleUUID();
        String providerCode = CharSequenceUtil.isNotBlank(aiChatProperty.getSummary().getCompressModelProvider())
                ? aiChatProperty.getSummary().getCompressModelProvider().trim()
                : fallbackProviderCode;
        String modelCode = CharSequenceUtil.isNotBlank(aiChatProperty.getSummary().getCompressModelCode())
                ? aiChatProperty.getSummary().getCompressModelCode().trim()
                : fallbackModelCode;

        com.vita.aichat.aiChatRequest.entity.AiChatRequest compressRequest = new com.vita.aichat.aiChatRequest.entity.AiChatRequest();
        compressRequest.setId(SnowUtils.getSnowflakeNextId());
        compressRequest.setConversationId(conversation.getId());
        compressRequest.setUserId(userId);
        compressRequest.setRequestId(requestId);
        compressRequest.setProviderCode(providerCode);
        compressRequest.setRequestScene(AiChatRequestSceneEnum.SUMMARY_COMPRESS.name());
        compressRequest.setRequestMode(AiChatRequestModeEnum.SYNC.name());
        compressRequest.setStatus(AiChatRequestStatusEnum.PENDING.name());
        compressRequest.setModelCode(modelCode);
        compressRequest.setStartedAt(LocalDateTime.now());
        aiChatRequestService.create(compressRequest);

        markConversationSummaryStatus(conversation.getId(), AiSummaryStatusEnum.GENERATING);

        try {
            AiChatResponse response = aiChatClient.chat(buildCompressRequest(
                    conversation,
                    userId,
                    requestId,
                    providerCode,
                    modelCode,
                    pendingSummaryMessages
            ));

            if (response == null || CharSequenceUtil.isBlank(response.getContent())) {
                throw new IllegalStateException("SUMMARY_COMPRESS_EMPTY");
            }

            transactionTemplate.executeWithoutResult(status -> {
                AiConversationSummary latestSummary = aiConversationSummaryService.getLatestSummary(conversation.getId(), userId);
                int nextVersion = latestSummary == null || latestSummary.getVersionNo() == null
                        ? 1
                        : latestSummary.getVersionNo() + 1;
                aiConversationSummaryService.markReadySummariesStale(conversation.getId(), userId);

                AiConversationSummary summary = new AiConversationSummary();
                summary.setId(SnowUtils.getSnowflakeNextId());
                summary.setConversationId(conversation.getId());
                summary.setUserId(userId);
                summary.setVersionNo(nextVersion);
                summary.setProviderCode(providerCode);
                summary.setModelCode(modelCode);
                summary.setSourceMessageStartId(pendingSummaryMessages.get(0).getId());
                summary.setSourceMessageEndId(pendingSummaryMessages.get(pendingSummaryMessages.size() - 1).getId());
                summary.setSourceMessageCount(pendingSummaryMessages.size());
                summary.setSummaryText(response.getContent());
                summary.setSummaryJson(null);
                summary.setTokenSavedEstimate(response.getTokenTotal());
                summary.setStatus(AiSummaryStatusEnum.READY.name());
                aiConversationSummaryService.create(summary);

                com.vita.aichat.aiChatRequest.entity.AiChatRequest update = new com.vita.aichat.aiChatRequest.entity.AiChatRequest();
                update.setId(compressRequest.getId());
                update.setProviderRequestId(response.getProviderRequestId());
                update.setStatus(AiChatRequestStatusEnum.SUCCESS.name());
                update.setFinishReason(response.getFinishReason());
                update.setTokenInput(response.getTokenInput());
                update.setTokenOutput(response.getTokenOutput());
                update.setTokenTotal(response.getTokenTotal());
                update.setFirstTokenAt(LocalDateTime.now());
                update.setCompletedAt(LocalDateTime.now());
                aiChatRequestService.update(update);

                AiConversation conversationUpdate = new AiConversation();
                conversationUpdate.setId(conversation.getId());
                conversationUpdate.setSummaryVersion(nextVersion);
                conversationUpdate.setSummaryStatus(AiSummaryStatusEnum.READY.name());
                aiConversationService.update(conversationUpdate);
            });
        } catch (Exception ex) {
            ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(ex);
            transactionTemplate.executeWithoutResult(status -> {
                com.vita.aichat.aiChatRequest.entity.AiChatRequest update = new com.vita.aichat.aiChatRequest.entity.AiChatRequest();
                update.setId(compressRequest.getId());
                update.setStatus(AiChatRequestStatusEnum.ERROR.name());
                update.setCompletedAt(LocalDateTime.now());
                update.setErrorCode(String.valueOf(serviceException.getCode()));
                update.setErrorMessage(serviceException.getMessage());
                aiChatRequestService.update(update);
                AiConversation conversationUpdate = new AiConversation();
                conversationUpdate.setId(conversation.getId());
                conversationUpdate.setSummaryStatus(AiSummaryStatusEnum.STALE.name());
                aiConversationService.update(conversationUpdate);
            });
        }
    }

    private com.vita.ai.core.model.AiChatRequest buildCompressRequest(AiConversation conversation,
                                                                      Long userId,
                                                                      String requestId,
                                                                      String providerCode,
                                                                      String modelCode,
                                                                      List<AiMessage> summaryMessages) {
        List<AiChatMessage> messages = summaryMessages.stream()
                .map(message -> AiChatMessage.builder()
                        .role("system")
                        .content(message.getContent())
                        .build())
                .toList();
        return com.vita.ai.core.model.AiChatRequest.builder()
                .requestId(requestId)
                .module("aichat")
                .conversationId(conversation.getId())
                .userId(userId)
                .provider(providerCode)
                .model(modelCode)
                .systemPrompt("请将以下多条历史摘要进一步压缩为一条面向后续对话的长期记忆摘要，保留核心事实、用户偏好、约束和未完成事项。")
                .messages(messages)
                .userPrompt("请输出一条紧凑、清晰、可用于后续上下文注入的压缩摘要。")
                .stream(false)
                .build();
    }

    private void markConversationSummaryStatus(Long conversationId, AiSummaryStatusEnum summaryStatus) {
        AiConversation update = new AiConversation();
        update.setId(conversationId);
        update.setSummaryStatus(summaryStatus.name());
        aiConversationService.update(update);
    }
}
