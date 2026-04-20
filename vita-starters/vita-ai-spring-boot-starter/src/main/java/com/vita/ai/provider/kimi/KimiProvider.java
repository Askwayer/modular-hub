package com.vita.ai.provider.kimi;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.model.AiChatMessage;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import com.vita.ai.core.provider.AiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class KimiProvider implements AiProvider {

    private final ChatModel chatModel;

    @Override
    public String providerCode() {
        return "kimi";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        ChatResponse response = chatModel.call(toPrompt(request));
        return toAiChatResponse(request.getRequestId(), request.getModel(), response);
    }

    @Override
    public Flux<AiStreamChunk> streamChat(AiChatRequest request) {
        return chatModel.stream(toPrompt(request))
                .map(response -> toAiStreamChunk(request.getRequestId(), request.getModel(), response));
    }

    private Prompt toPrompt(AiChatRequest request) {
        List<Message> messages = new ArrayList<>();
        if (CharSequenceUtil.isNotBlank(request.getSystemPrompt())) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        if (request.getMessages() != null) {
            for (AiChatMessage msg : request.getMessages()) {
                messages.add(toSpringAiMessage(msg));
            }
        }
        if (CharSequenceUtil.isNotBlank(request.getUserPrompt())) {
            messages.add(new UserMessage(request.getUserPrompt()));
        }
        return new Prompt(messages, buildOptions(request));
    }

    private Message toSpringAiMessage(AiChatMessage msg) {
        return switch (msg.getRole().toLowerCase()) {
            case "system" -> new SystemMessage(msg.getContent());
            case "assistant" -> new AssistantMessage(msg.getContent());
            default -> new UserMessage(msg.getContent());
        };
    }

    private OpenAiChatOptions buildOptions(AiChatRequest request) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder().model(request.getModel());
        if (request.getMaxTokens() != null) {
            builder.maxTokens(request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature().doubleValue());
        }
        return builder.build();
    }

    private AiChatResponse toAiChatResponse(String requestId, String model, ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return AiChatResponse.builder().requestId(requestId).provider(providerCode()).build();
        }

        AssistantMessage output = response.getResult().getOutput();
        ChatGenerationMetadata generationMetadata = response.getResult().getMetadata();
        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;

        return AiChatResponse.builder()
                .requestId(requestId)
                .provider(providerCode())
                .providerRequestId(response.getMetadata() != null ? response.getMetadata().getId() : null)
                .model(model)
                .content(output.getText())
                .reasoningContent(extractReasoningContent(output))
                .finishReason(generationMetadata != null ? generationMetadata.getFinishReason() : null)
                .tokenInput(usage != null ? usage.getPromptTokens() : null)
                .tokenOutput(usage != null ? usage.getCompletionTokens() : null)
                .tokenTotal(usage != null ? usage.getTotalTokens() : null)
                .build();
    }

    private AiStreamChunk toAiStreamChunk(String requestId, String model, ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return AiStreamChunk.builder().requestId(requestId).finished(false).build();
        }

        AssistantMessage output = response.getResult().getOutput();
        ChatGenerationMetadata generationMetadata = response.getResult().getMetadata();
        String finishReason = generationMetadata != null ? generationMetadata.getFinishReason() : null;
        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;

        return AiStreamChunk.builder()
                .requestId(requestId)
                .providerRequestId(response.getMetadata() != null ? response.getMetadata().getId() : null)
                .delta(output.getText())
                .reasoningDelta(extractReasoningContent(output))
                .finished(CharSequenceUtil.isNotBlank(finishReason) && !"null".equals(finishReason))
                .finishReason(finishReason)
                .tokenInput(usage != null ? usage.getPromptTokens() : null)
                .tokenOutput(usage != null ? usage.getCompletionTokens() : null)
                .tokenTotal(usage != null ? usage.getTotalTokens() : null)
                .build();
    }

    private String extractReasoningContent(AssistantMessage message) {
        if (message == null) {
            return null;
        }
        Map<String, Object> metadata = message.getMetadata();
        if (metadata == null || !metadata.containsKey("reasoningContent")) {
            return null;
        }
        Object reasoningContent = metadata.get("reasoningContent");
        return reasoningContent != null ? String.valueOf(reasoningContent) : null;
    }
}
