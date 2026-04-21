package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.vita.ai.core.client.AiChatClient;
import com.vita.ai.core.model.AiChatMessage;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.aiRoundMemory.entity.AiRoundMemory;
import com.vita.aichat.aiRoundMemory.service.IAiRoundMemoryService;
import com.vita.aichat.chat.model.memory.AiMemoryPayload;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.enums.*;
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
    private final IAiRoundMemoryService aiRoundMemoryService;
    private final AiChatClient aiChatClient;
    private final AiChatProperty aiChatProperty;
    private final AiChatExceptionConverter aiChatExceptionConverter;
    private final ConversationSummaryCompressService conversationSummaryCompressService;
    private final ConversationMemoryFormatter conversationMemoryFormatter;
    private final ContextTokenEstimator contextTokenEstimator;
    private final TransactionTemplate transactionTemplate;

    public RoundSummaryService(IAiMessageService aiMessageService,
                               IAiChatRequestService aiChatRequestService,
                               IAiConversationService aiConversationService,
                               IAiRoundMemoryService aiRoundMemoryService,
                               AiChatClient aiChatClient,
                               AiChatProperty aiChatProperty,
                               AiChatExceptionConverter aiChatExceptionConverter,
                               ConversationSummaryCompressService conversationSummaryCompressService,
                               ConversationMemoryFormatter conversationMemoryFormatter,
                               ContextTokenEstimator contextTokenEstimator,
                               TransactionTemplate transactionTemplate) {
        this.aiMessageService = aiMessageService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiConversationService = aiConversationService;
        this.aiRoundMemoryService = aiRoundMemoryService;
        this.aiChatClient = aiChatClient;
        this.aiChatProperty = aiChatProperty;
        this.aiChatExceptionConverter = aiChatExceptionConverter;
        this.conversationSummaryCompressService = conversationSummaryCompressService;
        this.conversationMemoryFormatter = conversationMemoryFormatter;
        this.contextTokenEstimator = contextTokenEstimator;
        this.transactionTemplate = transactionTemplate;
    }

    public void generateAndCompressIfNeeded(AiChatRoundSummaryEvent event) {
        if (!Boolean.TRUE.equals(aiChatProperty.getMemory().getRoundMemoryEnabled())) {
            return;
        }
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
                AiMemoryPayload memoryPayload = normalizeMemoryPayload(response.getContent());
                AiRoundMemory latestRoundMemory = aiRoundMemoryService.getLatestRoundMemory(conversation.getId(), event.getUserId());
                int roundNo = resolveRoundNo(conversation, latestRoundMemory);
                AiRoundMemory roundMemory = buildRoundMemory(
                        conversation,
                        event,
                        requestId,
                        providerCode,
                        modelCode,
                        userMessage,
                        assistantMessage,
                        memoryPayload,
                        response
                );
                roundMemory.setRoundNo(roundNo);
                aiRoundMemoryService.create(roundMemory);

                com.vita.aichat.aiChatRequest.entity.AiChatRequest update = new com.vita.aichat.aiChatRequest.entity.AiChatRequest();
                update.setId(summaryRequest.getId());
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

            conversationSummaryCompressService.compressIfNeeded(
                    conversation,
                    event.getUserId(),
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
                .systemPrompt("""
                        请将当前这一轮对话提炼为一条后续可复用的内部短期记忆。
                        你必须只输出一个 JSON 对象，不要输出 markdown，不要补充解释。
                        JSON 字段：
                        - memoryText: 一条紧凑中文记忆
                        - facts: 关键事实数组
                        - preferences: 用户偏好数组
                        - constraints: 限制条件数组
                        - openLoops: 仍需延续的问题数组
                        - pendingTasks: 待办数组
                        - disallowed: 明确禁忌数组
                        - carryForward: 布尔值，表示后续几轮是否应继续注入
                        - salienceScore: 0 到 100 的整数，表示记忆显著性
                        - carryForwardReason: 若 carryForward=true，给出一句原因
                        """)
                .messages(List.of(
                        AiChatMessage.builder().role("user").content(userMessage.getContent()).build(),
                        AiChatMessage.builder().role("assistant").content(assistantMessage.getContent()).build()
                ))
                .userPrompt("请只输出 JSON。")
                .stream(false)
                .build();
    }

    private AiMemoryPayload normalizeMemoryPayload(String responseContent) {
        AiMemoryPayload payload = conversationMemoryFormatter.parsePayload(responseContent);
        if (payload == null) {
            payload = new AiMemoryPayload();
            payload.setMemoryText(responseContent.trim());
        }
        if (CharSequenceUtil.isBlank(payload.getMemoryText()) && CharSequenceUtil.isNotBlank(payload.getSummaryText())) {
            payload.setMemoryText(payload.getSummaryText().trim());
        }
        if (payload.getCarryForward() == null) {
            payload.setCarryForward(hasCarryForwardSignals(payload));
        }
        if (payload.getSalienceScore() == null) {
            payload.setSalienceScore(defaultSalienceScore(payload));
        }
        return payload;
    }

    private AiRoundMemory buildRoundMemory(AiConversation conversation,
                                           AiChatRoundSummaryEvent event,
                                           String requestId,
                                           String providerCode,
                                           String modelCode,
                                           AiMessage userMessage,
                                           AiMessage assistantMessage,
                                           AiMemoryPayload payload,
                                           AiChatResponse response) {
        AiRoundMemory roundMemory = new AiRoundMemory();
        roundMemory.setId(SnowUtils.getSnowflakeNextId());
        roundMemory.setConversationId(conversation.getId());
        roundMemory.setUserId(event.getUserId());
        roundMemory.setSourceRequestId(userMessage.getRequestId());
        roundMemory.setSourceUserMessageId(userMessage.getId());
        roundMemory.setSourceAssistantMessageId(assistantMessage.getId());
        roundMemory.setProviderCode(providerCode);
        roundMemory.setModelCode(modelCode);
        roundMemory.setMemoryText(CharSequenceUtil.blankToDefault(payload.getMemoryText(), response.getContent()).trim());
        roundMemory.setMemoryJson(JSON.toJSONString(payload));
        roundMemory.setCarryForward(Boolean.TRUE.equals(payload.getCarryForward()) ? (byte) 1 : (byte) 0);
        roundMemory.setSalienceScore(payload.getSalienceScore());
        roundMemory.setStatus(resolveRoundMemoryStatus(payload).name());
        roundMemory.setPromptTokenEstimate(estimateRoundMemoryTokens(roundMemory));
        return roundMemory;
    }

    private int resolveRoundNo(AiConversation conversation, AiRoundMemory latestRoundMemory) {
        int conversationRoundNo = conversation.getRoundCount() == null ? 0 : conversation.getRoundCount();
        int latestRoundNo = latestRoundMemory == null || latestRoundMemory.getRoundNo() == null ? 0 : latestRoundMemory.getRoundNo();
        return Math.max(conversationRoundNo, latestRoundNo + 1);
    }

    private AiRoundMemoryStatusEnum resolveRoundMemoryStatus(AiMemoryPayload payload) {
        boolean carryForward = Boolean.TRUE.equals(payload.getCarryForward());
        int salienceScore = payload.getSalienceScore() == null ? 0 : payload.getSalienceScore();
        if (!carryForward && salienceScore < aiChatProperty.getMemory().getSalienceThreshold()) {
            return AiRoundMemoryStatusEnum.SKIPPED;
        }
        return AiRoundMemoryStatusEnum.READY;
    }

    private int estimateRoundMemoryTokens(AiRoundMemory roundMemory) {
        String renderedMemory = conversationMemoryFormatter.renderRoundMemory(roundMemory);
        if (CharSequenceUtil.isBlank(renderedMemory)) {
            return contextTokenEstimator.estimateText(roundMemory.getMemoryText());
        }
        return contextTokenEstimator.estimateText(renderedMemory);
    }

    private boolean hasCarryForwardSignals(AiMemoryPayload payload) {
        return (payload.getOpenLoops() != null && !payload.getOpenLoops().isEmpty())
                || (payload.getPendingTasks() != null && !payload.getPendingTasks().isEmpty())
                || (payload.getConstraints() != null && !payload.getConstraints().isEmpty());
    }

    private int defaultSalienceScore(AiMemoryPayload payload) {
        int score = 40;
        score += sizeScore(payload.getFacts());
        score += sizeScore(payload.getPreferences());
        score += sizeScore(payload.getConstraints());
        score += sizeScore(payload.getOpenLoops());
        score += sizeScore(payload.getPendingTasks());
        if (Boolean.TRUE.equals(payload.getCarryForward())) {
            score += 20;
        }
        return Math.min(score, 100);
    }

    private int sizeScore(List<String> values) {
        return values == null ? 0 : Math.min(values.size() * 8, 24);
    }
}
