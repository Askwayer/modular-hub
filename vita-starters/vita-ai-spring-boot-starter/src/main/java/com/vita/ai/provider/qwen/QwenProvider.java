package com.vita.ai.provider.qwen;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.vita.ai.core.model.AiChatMessage;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import com.vita.ai.core.provider.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class QwenProvider implements AiProvider {

    private final ChatModel chatModel;

    @Override
    public String providerCode() {
        return "qwen";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        Prompt prompt = toPrompt(request);
        ChatResponse response = chatModel.call(prompt);
        return toAiChatResponse(request.getRequestId(), request.getModel(), response);
    }

    @Override
    public Flux<AiStreamChunk> streamChat(AiChatRequest request) {
        Prompt prompt = toPrompt(request);
        return chatModel.stream(prompt)
                .map(chatResponse -> toAiStreamChunk(request.getRequestId(), request.getModel(), chatResponse));
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

        return new Prompt(messages, buildDashScopeOptions(request));
    }

    private Message toSpringAiMessage(AiChatMessage msg) {
        return switch (msg.getRole().toLowerCase()) {
            case "system" -> new SystemMessage(msg.getContent());
            case "assistant" -> new AssistantMessage(msg.getContent());
            default -> new UserMessage(msg.getContent());
        };
    }

    private DashScopeChatOptions buildDashScopeOptions(AiChatRequest request) {
        var builder = DashScopeChatOptions.builder()
                .model(request.getModel())
                .stream(Boolean.TRUE.equals(request.getStream()))
                .incrementalOutput(Boolean.TRUE.equals(request.getStream()))
                .enableSearch(true)
                .enableThinking(isThinkingModel(request.getModel()));

        if (request.getMaxTokens() != null) {
            builder.maxToken(request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature().doubleValue());
        }
        return builder.build();
    }

    private boolean isThinkingModel(String modelCode) {
        return CharSequenceUtil.isNotBlank(modelCode) && modelCode.startsWith("qwq-");
    }

    private AiChatResponse toAiChatResponse(String requestId, String model, ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return AiChatResponse.builder()
                    .requestId(requestId)
                    .provider(providerCode())
                    .build();
        }

        AssistantMessage output = response.getResult().getOutput();
        String content = output.getText();
        String reasoningContent = extractReasoningContent(output);

        ChatGenerationMetadata generationMetadata = response.getResult().getMetadata();
        String finishReason = generationMetadata != null ? generationMetadata.getFinishReason() : null;

        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;

        return AiChatResponse.builder()
                .requestId(requestId)
                .provider(providerCode())
                .providerRequestId(response.getMetadata() != null ? response.getMetadata().getId() : null)
                .model(model)
                .content(content)
                .reasoningContent(reasoningContent)
                .finishReason(finishReason)
                .tokenInput(usage != null ? usage.getPromptTokens() : null)
                .tokenOutput(usage != null ? usage.getCompletionTokens() : null)
                .tokenTotal(usage != null ? usage.getTotalTokens() : null)
                .build();
    }

    private AiStreamChunk toAiStreamChunk(String requestId, String model, ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return AiStreamChunk.builder()
                    .requestId(requestId)
                    .finished(false)
                    .build();
        }

        AssistantMessage output = response.getResult().getOutput();
        String delta = output.getText();
        String reasoningDelta = extractReasoningContent(output);

        ChatGenerationMetadata generationMetadata = response.getResult().getMetadata();
        String finishReason = generationMetadata != null ? generationMetadata.getFinishReason() : null;
        boolean finished = CharSequenceUtil.isNotBlank(finishReason) && !"null".equals(finishReason);

        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;

        return AiStreamChunk.builder()
                .requestId(requestId)
                .providerRequestId(response.getMetadata() != null ? response.getMetadata().getId() : null)
                .delta(delta)
                .reasoningDelta(reasoningDelta)
                .finished(finished)
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
        if (metadata != null && metadata.containsKey("reasoningContent")) {
            Object reasoningContent = metadata.get("reasoningContent");
            return reasoningContent != null ? String.valueOf(reasoningContent) : null;
        }
        return null;
    }
}
