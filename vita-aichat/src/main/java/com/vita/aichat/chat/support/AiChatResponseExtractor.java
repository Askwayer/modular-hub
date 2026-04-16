package com.vita.aichat.chat.support;

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

    public AiChatResponseChunk extractChunk(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return new AiChatResponseChunk();
        }
        AiChatResponseChunk chunk = new AiChatResponseChunk();
        AssistantMessage assistantMessage = response.getResult().getOutput();
        chunk.setContentDelta(assistantMessage.getText());
        chunk.setProviderRequestId(response.getMetadata() != null ? response.getMetadata().getId() : null);

        Map<String, Object> metadata = assistantMessage.getMetadata();
        if (metadata != null) {
            Object reasoningContent = metadata.get("reasoningContent");
            if (reasoningContent != null) {
                chunk.setReasoningDelta(String.valueOf(reasoningContent));
            }
        }

        ChatGenerationMetadata generationMetadata = response.getResult().getMetadata();
        if (generationMetadata != null) {
            chunk.setFinishReason(generationMetadata.getFinishReason());
        }

        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
        if (usage != null) {
            chunk.setTokenInput(usage.getPromptTokens());
            chunk.setTokenOutput(usage.getCompletionTokens());
            chunk.setTokenTotal(usage.getTotalTokens());
        }
        return chunk;
    }

    public AiChatCompletionResult extractCompletion(ChatResponse response) {
        AiChatResponseChunk chunk = extractChunk(response);
        AiChatCompletionResult result = new AiChatCompletionResult();
        result.setProviderRequestId(chunk.getProviderRequestId());
        result.setContent(chunk.getContentDelta());
        result.setReasoningContent(chunk.getReasoningDelta());
        result.setFinishReason(chunk.getFinishReason());
        result.setTokenInput(chunk.getTokenInput());
        result.setTokenOutput(chunk.getTokenOutput());
        result.setTokenTotal(chunk.getTokenTotal());
        return result;
    }
}
