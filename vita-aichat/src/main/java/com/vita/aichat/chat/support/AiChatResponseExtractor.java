package com.vita.aichat.chat.support;

import cn.hutool.core.map.MapUtil;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.model.result.AiChatResponseChunk;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring AI 响应提取器。
 */
@Component
public class AiChatResponseExtractor {

    /**
     * 从模型响应中提取单个片段。
     *
     * @param response 模型响应对象
     * @return 响应片段
     */
    public AiChatResponseChunk extractChunk(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return new AiChatResponseChunk();
        }
        AssistantMessage assistantMessage = response.getResult().getOutput();

        Map<String, Object> metadata = assistantMessage.getMetadata();
        String reasoningDelta = null;
        if (MapUtil.isNotEmpty(metadata)) {
            Object reasoningContent = metadata.get("reasoningContent");
            if (reasoningContent != null) {
                reasoningDelta = String.valueOf(reasoningContent);
            }
        }

        ChatGenerationMetadata generationMetadata = response.getResult().getMetadata();
        String finishReason = generationMetadata != null ? generationMetadata.getFinishReason() : null;

        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
        Integer tokenInput = usage != null ? usage.getPromptTokens() : null;
        Integer tokenOutput = usage != null ? usage.getCompletionTokens() : null;
        Integer tokenTotal = usage != null ? usage.getTotalTokens() : null;

        return AiChatResponseChunk.builder()
                .contentDelta(assistantMessage.getText())
                .providerRequestId(response.getMetadata() != null ? response.getMetadata().getId() : null)
                .reasoningDelta(reasoningDelta)
                .finishReason(finishReason)
                .tokenInput(tokenInput)
                .tokenOutput(tokenOutput)
                .tokenTotal(tokenTotal)
                .build();
    }

    /**
     * 从模型响应中提取完整完成结果。
     *
     * @param response 模型响应对象
     * @return 完成结果
     */
    public AiChatCompletionResult extractCompletion(ChatResponse response) {
        AiChatResponseChunk chunk = extractChunk(response);
        return AiChatCompletionResult.builder()
                .providerRequestId(chunk.getProviderRequestId())
                .content(chunk.getContentDelta())
                .reasoningContent(chunk.getReasoningDelta())
                .finishReason(chunk.getFinishReason())
                .tokenInput(chunk.getTokenInput())
                .tokenOutput(chunk.getTokenOutput())
                .tokenTotal(chunk.getTokenTotal())
                .build();
    }
}
