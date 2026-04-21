package com.vita.ai.provider.fastapi;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.model.AiChatResponse;
import com.vita.ai.core.model.AiStreamChunk;
import com.vita.ai.core.provider.AiProvider;
import com.vita.ai.provider.fastapi.property.FastApiProviderProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class FastApiProvider implements AiProvider {

    private final FastApiProviderProperty property;
    private final WebClient webClient;

    @Override
    public String providerCode() {
        return "fastapi";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        FastApiChatResponse response = webClient.post()
                .uri(property.getChatPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildPayload(request))
                .retrieve()
                .bodyToMono(FastApiChatResponse.class)
                .block();
        if (response == null) {
            return AiChatResponse.builder()
                    .requestId(request.getRequestId())
                    .provider(providerCode())
                    .model(request.getModel())
                    .build();
        }
        return AiChatResponse.builder()
                .requestId(request.getRequestId())
                .provider(providerCode())
                .providerRequestId(response.requestId)
                .model(CharSequenceUtil.isNotBlank(response.model) ? response.model : request.getModel())
                .content(response.content)
                .reasoningContent(response.reasoningContent)
                .finishReason(response.finishReason)
                .tokenInput(response.tokenInput)
                .tokenOutput(response.tokenOutput)
                .tokenTotal(response.tokenTotal)
                .rawResponse(response.rawResponse)
                .build();
    }

    @Override
    public Flux<AiStreamChunk> streamChat(AiChatRequest request) {
        return webClient.post()
                .uri(property.getStreamPath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON, MediaType.TEXT_EVENT_STREAM)
                .bodyValue(buildPayload(request))
                .retrieve()
                .bodyToFlux(FastApiStreamResponse.class)
                .map(response -> AiStreamChunk.builder()
                        .requestId(request.getRequestId())
                        .providerRequestId(response.requestId)
                        .delta(response.delta)
                        .reasoningDelta(response.reasoningDelta)
                        .finished(Boolean.TRUE.equals(response.finished))
                        .finishReason(response.finishReason)
                        .tokenInput(response.tokenInput)
                        .tokenOutput(response.tokenOutput)
                        .tokenTotal(response.tokenTotal)
                        .rawChunk(response.rawChunk)
                        .build());
    }

    private Map<String, Object> buildPayload(AiChatRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", request.getRequestId());
        payload.put("module", request.getModule());
        payload.put("provider", request.getProvider());
        payload.put("model", CharSequenceUtil.isBlank(request.getModel()) ? property.getDefaultModel() : request.getModel());
        payload.put("systemPrompt", request.getSystemPrompt());
        payload.put("userPrompt", request.getUserPrompt());
        payload.put("messages", request.getMessages() == null ? List.of() : request.getMessages());
        payload.put("temperature", request.getTemperature());
        payload.put("maxTokens", request.getMaxTokens());
        payload.put("stream", Boolean.TRUE.equals(request.getStream()));
        payload.put("options", request.getOptions());
        return payload;
    }

    private static class FastApiChatResponse {
        public String requestId;
        public String model;
        public String content;
        public String reasoningContent;
        public String finishReason;
        public Integer tokenInput;
        public Integer tokenOutput;
        public Integer tokenTotal;
        public String rawResponse;
    }

    private static class FastApiStreamResponse {
        public String requestId;
        public String delta;
        public String reasoningDelta;
        public Boolean finished;
        public String finishReason;
        public Integer tokenInput;
        public Integer tokenOutput;
        public Integer tokenTotal;
        public String rawChunk;
    }
}
