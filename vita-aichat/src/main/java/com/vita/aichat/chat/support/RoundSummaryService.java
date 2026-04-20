package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.ai.core.client.AiChatClient;
import com.vita.ai.core.model.AiChatMessage;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
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
 * 每轮问答成功后的摘要生成服务。
 */
@Service
public class RoundSummaryService {

    private final IAiMessageService aiMessageService;
    private final IAiChatRequestService aiChatRequestService;
    private final IAiConversationService aiConversationService;
    private final IAiConversationSummaryService aiConversationSummaryService;
    private final AiChatClient aiChatClient;
    private final AiChatMessageBuilder aiChatMessageBuilder;
    private final AiChatProperty aiChatProperty;
    private final AiChatExceptionConverter aiChatExceptionConverter;
    private final ConversationSummaryCompressService conversationSummaryCompressService;
    private final TransactionTemplate transactionTemplate;

    public RoundSummaryService(IAiMessageService aiMessageService,
                               IAiChatRequestService aiChatRequestService,
                               IAiConversationService aiConversationService,
                               IAiConversationSummaryService aiConversationSummaryService,
                               AiChatClient aiChatClient,
                               AiChatMessageBuilder aiChatMessageBuilder,
                               AiChatProperty aiChatProperty,
                               AiChatExceptionConverter aiChatExceptionConverter,
                               ConversationSummaryCompressService conversationSummaryCompressService,
                               TransactionTemplate transactionTemplate) {
        this.aiMessageService = aiMessageService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiConversationService = aiConversationService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiChatClient = aiChatClient;
        this.aiChatMessageBuilder = aiChatMessageBuilder;
        this.aiChatProperty = aiChatProperty;
        this.aiChatExceptionConverter = aiChatExceptionConverter;
        this.conversationSummaryCompressService = conversationSummaryCompressService;
        this.transactionTemplate = transactionTemplate;
    }

    public void generateAndCompressIfNeeded(AiChatRoundSummaryEvent event) {
        AiConversation conversation = aiConversationService.getByIdAndUserId(event.getConversationId(), event.getUserId());
        if (conversation == null) {
            return;
        }
        AiMessage userMessage = aiMessageService.getByIdAndUserId(event.getUserMessageId(), event.getUserId());
        AiMessage assistantMessage = aiMessageService.getByIdAndUserId(event.getAssistantMessageId(), event.getUserId());
        if (userMessage == null || assistantMessage == null || CharSequenceUtil.isBlank(assistantMessage.getContent())) {
            return;
        }

        String requestId = IdUtil.fastSimpleUUID();
        String providerCode = CharSequenceUtil.isNotBlank(aiChatProperty.getSummary().getRoundSummaryProvider())
                ? aiChatProperty.getSummary().getRoundSummaryProvider().trim()
                : event.getProviderCode();
        String modelCode = CharSequenceUtil.isNotBlank(aiChatProperty.getSummary().getRoundSummaryModelCode())
                ? aiChatProperty.getSummary().getRoundSummaryModelCode().trim()
                : event.getModelCode();

        com.vita.aichat.aiChatRequest.entity.AiChatRequest summaryRequest = new com.vita.aichat.aiChatRequest.entity.AiChatRequest();
        summaryRequest.setId(SnowUtils.getSnowflakeNextId());
        summaryRequest.setConversationId(conversation.getId());
        summaryRequest.setUserId(event.getUserId());
        summaryRequest.setRequestId(requestId);
        summaryRequest.setProviderCode(providerCode);
        summaryRequest.setRequestScene(AiChatRequestSceneEnum.ROUND_SUMMARY.name());
        summaryRequest.setRequestMode(AiChatRequestModeEnum.SYNC.name());
        summaryRequest.setStatus(AiChatRequestStatusEnum.PENDING.name());
        summaryRequest.setUserMessageId(userMessage.getId());
        summaryRequest.setModelCode(modelCode);
        summaryRequest.setStartedAt(LocalDateTime.now());
        aiChatRequestService.create(summaryRequest);

        try {
            AiChatResponse response = aiChatClient.chat(buildRoundSummaryRequest(
                    conversation,
                    event.getUserId(),
                    requestId,
                    providerCode,
                    modelCode,
                    userMessage,
                    assistantMessage
            ));

            if (response == null || CharSequenceUtil.isBlank(response.getContent())) {
                throw new IllegalStateException("ROUND_SUMMARY_EMPTY");
            }

            transactionTemplate.executeWithoutResult(status -> {
                AiMessage summaryMessage = aiChatMessageBuilder.buildSummaryMessage(
                        conversation.getId(),
                        event.getUserId(),
                        requestId,
                        providerCode,
                        modelCode,
                        assistantMessage.getId(),
                        response.getContent()
                );
                summaryMessage.setProviderRequestId(response.getProviderRequestId());
                summaryMessage.setTokenInput(response.getTokenInput());
                summaryMessage.setTokenOutput(response.getTokenOutput());
                summaryMessage.setTokenTotal(response.getTokenTotal());
                summaryMessage.setFinishReason(response.getFinishReason());
                summaryMessage.setCompletedAt(LocalDateTime.now());
                summaryMessage.setFirstTokenAt(LocalDateTime.now());
                aiMessageService.create(summaryMessage);

                com.vita.aichat.aiChatRequest.entity.AiChatRequest update = new com.vita.aichat.aiChatRequest.entity.AiChatRequest();
                update.setId(summaryRequest.getId());
                update.setAssistantMessageId(summaryMessage.getId());
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
                conversationUpdate.setSummaryStatus(AiSummaryStatusEnum.STALE.name());
                aiConversationService.update(conversationUpdate);
            });

            AiConversationSummary latestSummary = aiConversationSummaryService.getLatestReadySummary(conversation.getId(), event.getUserId());
            Long latestCompressedSummaryMessageId = latestSummary != null ? latestSummary.getSourceMessageEndId() : null;
            List<AiMessage> pendingSummaryMessages = aiMessageService.listPendingSummaryMessages(
                    conversation.getId(),
                    event.getUserId(),
                    latestCompressedSummaryMessageId
            );
            conversationSummaryCompressService.compressIfNeeded(
                    conversation,
                    event.getUserId(),
                    pendingSummaryMessages,
                    providerCode,
                    modelCode
            );
        } catch (Exception ex) {
            ServiceException serviceException = aiChatExceptionConverter.convertToServiceException(ex);
            transactionTemplate.executeWithoutResult(status -> {
                com.vita.aichat.aiChatRequest.entity.AiChatRequest update = new com.vita.aichat.aiChatRequest.entity.AiChatRequest();
                update.setId(summaryRequest.getId());
                update.setStatus(AiChatRequestStatusEnum.ERROR.name());
                update.setCompletedAt(LocalDateTime.now());
                update.setErrorCode(String.valueOf(serviceException.getCode()));
                update.setErrorMessage(serviceException.getMessage());
                aiChatRequestService.update(update);
            });
        }
    }

    private AiChatRequest buildRoundSummaryRequest(AiConversation conversation,
                                                   Long userId,
                                                   String requestId,
                                                   String providerCode,
                                                   String modelCode,
                                                   AiMessage userMessage,
                                                   AiMessage assistantMessage) {
        return AiChatRequest.builder()
                .requestId(requestId)
                .module("aichat")
                .conversationId(conversation.getId())
                .userId(userId)
                .provider(providerCode)
                .model(modelCode)
                .systemPrompt("请将当前这一轮对话提炼为一条后续可复用的会话记忆。摘要要简洁，保留事实、用户意图、限制条件、偏好和待办。")
                .messages(List.of(
                        AiChatMessage.builder().role("user").content(userMessage.getContent()).build(),
                        AiChatMessage.builder().role("assistant").content(assistantMessage.getContent()).build()
                ))
                .userPrompt("请输出一条简明轮次摘要。")
                .stream(false)
                .build();
    }
}
