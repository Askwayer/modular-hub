package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.vita.ai.core.client.AiChatClient;
import com.vita.ai.core.model.AiChatMessage;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.aichat.aiChatRequest.service.IAiChatRequestService;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
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
 * 多条轮次摘要再次压缩为会话级摘要。
 */
@Service
public class ConversationSummaryCompressService {

    private final IAiConversationSummaryService aiConversationSummaryService;
    private final IAiRoundMemoryService aiRoundMemoryService;
    private final IAiConversationService aiConversationService;
    private final IAiChatRequestService aiChatRequestService;
    private final AiChatClient aiChatClient;
    private final AiChatProperty aiChatProperty;
    private final AiChatExceptionConverter aiChatExceptionConverter;
    private final ConversationMemoryFormatter conversationMemoryFormatter;
    private final ContextTokenEstimator contextTokenEstimator;
    private final TransactionTemplate transactionTemplate;

    public ConversationSummaryCompressService(IAiConversationSummaryService aiConversationSummaryService,
                                              IAiRoundMemoryService aiRoundMemoryService,
                                              IAiConversationService aiConversationService,
                                              IAiChatRequestService aiChatRequestService,
                                              AiChatClient aiChatClient,
                                              AiChatProperty aiChatProperty,
                                              AiChatExceptionConverter aiChatExceptionConverter,
                                              ConversationMemoryFormatter conversationMemoryFormatter,
                                              ContextTokenEstimator contextTokenEstimator,
                                              TransactionTemplate transactionTemplate) {
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiRoundMemoryService = aiRoundMemoryService;
        this.aiConversationService = aiConversationService;
        this.aiChatRequestService = aiChatRequestService;
        this.aiChatClient = aiChatClient;
        this.aiChatProperty = aiChatProperty;
        this.aiChatExceptionConverter = aiChatExceptionConverter;
        this.conversationMemoryFormatter = conversationMemoryFormatter;
        this.contextTokenEstimator = contextTokenEstimator;
        this.transactionTemplate = transactionTemplate;
    }

    public void compressIfNeeded(AiConversation conversation,
                                 Long userId,
                                 String fallbackProviderCode,
                                 String fallbackModelCode) {
        if (!Boolean.TRUE.equals(aiChatProperty.getSummary().getCompressEnabled())) {
            return;
        }
        List<AiRoundMemory> readyRoundMemories = aiRoundMemoryService.listReadyRollupCandidates(conversation.getId(), userId);
        if (!shouldCompress(readyRoundMemories)) {
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
                    readyRoundMemories
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
                summary.setSourceMessageStartId(readyRoundMemories.get(0).getSourceUserMessageId());
                summary.setSourceMessageEndId(readyRoundMemories.get(readyRoundMemories.size() - 1).getSourceAssistantMessageId());
                summary.setSourceMessageCount(readyRoundMemories.size());
                summary.setSourceRoundStartNo(readyRoundMemories.get(0).getRoundNo());
                summary.setSourceRoundEndNo(readyRoundMemories.get(readyRoundMemories.size() - 1).getRoundNo());
                summary.setSourceRoundMemoryStartId(readyRoundMemories.get(0).getId());
                summary.setSourceRoundMemoryEndId(readyRoundMemories.get(readyRoundMemories.size() - 1).getId());
                AiMemoryPayload payload = normalizeSummaryPayload(response.getContent());
                summary.setSummaryText(resolveSummaryText(payload, response.getContent()));
                summary.setSummaryJson(JSON.toJSONString(payload));
                summary.setPromptTokenEstimate(estimateSummaryTokens(summary));
                summary.setTokenSavedEstimate(Math.max(sumPromptTokenEstimate(readyRoundMemories) - summary.getPromptTokenEstimate(), 0));
                summary.setStatus(AiSummaryStatusEnum.READY.name());
                aiConversationSummaryService.create(summary);
                markRoundMemoriesRolledUp(readyRoundMemories);

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
                                                                      List<AiRoundMemory> roundMemories) {
        List<AiChatMessage> messages = roundMemories.stream()
                .map(roundMemory -> AiChatMessage.builder()
                        .role("system")
                        .content(conversationMemoryFormatter.renderRoundMemory(roundMemory))
                        .build())
                .toList();
        return com.vita.ai.core.model.AiChatRequest.builder()
                .requestId(requestId)
                .module("aichat")
                .conversationId(conversation.getId())
                .userId(userId)
                .provider(providerCode)
                .model(modelCode)
                .systemPrompt("""
                        请将以下多条短期轮次记忆压缩为一条长期会话记忆。
                        你必须只输出一个 JSON 对象，不要输出 markdown，不要补充解释。
                        JSON 字段：
                        - summaryText: 一条紧凑中文长期记忆
                        - facts: 关键事实数组
                        - preferences: 用户偏好数组
                        - constraints: 限制条件数组
                        - openLoops: 仍需延续的问题数组
                        - pendingTasks: 待办数组
                        - disallowed: 明确禁忌数组
                        """)
                .messages(messages)
                .userPrompt("请只输出 JSON。")
                .stream(false)
                .build();
    }

    private void markConversationSummaryStatus(Long conversationId, AiSummaryStatusEnum summaryStatus) {
        AiConversation update = new AiConversation();
        update.setId(conversationId);
        update.setSummaryStatus(summaryStatus.name());
        aiConversationService.update(update);
    }

    private boolean shouldCompress(List<AiRoundMemory> readyRoundMemories) {
        if (readyRoundMemories == null || readyRoundMemories.isEmpty()) {
            return false;
        }
        if (readyRoundMemories.size() >= aiChatProperty.getMemory().getRollupTriggerRounds()) {
            return true;
        }
        return sumPromptTokenEstimate(readyRoundMemories) >= aiChatProperty.getMemory().getRollupTriggerTokens();
    }

    private int sumPromptTokenEstimate(List<AiRoundMemory> readyRoundMemories) {
        return readyRoundMemories.stream()
                .mapToInt(memory -> memory.getPromptTokenEstimate() == null ? 0 : memory.getPromptTokenEstimate())
                .sum();
    }

    private AiMemoryPayload normalizeSummaryPayload(String content) {
        AiMemoryPayload payload = conversationMemoryFormatter.parsePayload(content);
        if (payload == null) {
            payload = new AiMemoryPayload();
            payload.setSummaryText(content.trim());
        }
        if (CharSequenceUtil.isBlank(payload.getSummaryText()) && CharSequenceUtil.isNotBlank(payload.getMemoryText())) {
            payload.setSummaryText(payload.getMemoryText().trim());
        }
        return payload;
    }

    private String resolveSummaryText(AiMemoryPayload payload, String fallback) {
        return CharSequenceUtil.blankToDefault(payload.getSummaryText(), fallback).trim();
    }

    private int estimateSummaryTokens(AiConversationSummary summary) {
        String renderedSummary = conversationMemoryFormatter.renderLongMemory(summary);
        if (CharSequenceUtil.isBlank(renderedSummary)) {
            return contextTokenEstimator.estimateText(summary.getSummaryText());
        }
        return contextTokenEstimator.estimateText(renderedSummary);
    }

    private void markRoundMemoriesRolledUp(List<AiRoundMemory> readyRoundMemories) {
        List<AiRoundMemory> updates = readyRoundMemories.stream().map(memory -> {
            AiRoundMemory update = new AiRoundMemory();
            update.setId(memory.getId());
            update.setStatus(AiRoundMemoryStatusEnum.ROLLED_UP.name());
            return update;
        }).toList();
        aiRoundMemoryService.updateBatch(updates);
    }
}
