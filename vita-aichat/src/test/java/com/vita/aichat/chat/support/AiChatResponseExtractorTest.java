package com.vita.aichat.chat.support;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiChatResponseExtractorTest {

    private static class TestAssistantMessage extends AssistantMessage {

        TestAssistantMessage(String text, Map<String, Object> metadata) {
            super(text, metadata, List.of(), List.of());
        }
    }

    @Test
    void shouldExtractContentReasoningAndUsageFromChatResponse() {
        AssistantMessage assistantMessage = new TestAssistantMessage("正文分片", Map.of("reasoningContent", "推理分片"));
        ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
                .finishReason("stop")
                .build();
        ChatResponseMetadata responseMetadata = ChatResponseMetadata.builder()
                .id("provider-request-id")
                .usage(new DefaultUsage(11, 22, 33))
                .build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistantMessage, generationMetadata)), responseMetadata);

        AiChatResponseExtractor extractor = new AiChatResponseExtractor();

        var chunk = extractor.extractChunk(response);
        assertEquals("provider-request-id", chunk.getProviderRequestId());
        assertEquals("正文分片", chunk.getContentDelta());
        assertEquals("推理分片", chunk.getReasoningDelta());
        assertEquals("stop", chunk.getFinishReason());
        assertEquals(11, chunk.getTokenInput());
        assertEquals(22, chunk.getTokenOutput());
        assertEquals(33, chunk.getTokenTotal());
    }
}
