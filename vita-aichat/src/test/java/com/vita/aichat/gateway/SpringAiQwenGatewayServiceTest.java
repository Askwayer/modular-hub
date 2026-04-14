package com.vita.aichat.gateway;

import com.vita.aichat.chat.AiChatConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiQwenGatewayServiceTest {

    @Mock
    private ChatModel chatModel;

    @Test
    void chatShouldBuildPromptAndMapResponse() {
        SpringAiQwenGatewayService gatewayService = new SpringAiQwenGatewayService(chatModel);
        AiChatGatewayReq req = buildGatewayReq();
        when(chatModel.call(any(Prompt.class))).thenReturn(buildChatResponse("最终回答", "推理内容", 12, 23, 35));

        AiChatGatewayResp resp = gatewayService.chat(req);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        List<Message> messages = prompt.getInstructions();
        assertEquals(4, messages.size());
        assertEquals("system", messages.get(0).getMessageType().getValue());
        assertEquals("你是一个严谨的开发助手。", messages.get(0).getText());
        assertEquals("user", messages.get(1).getMessageType().getValue());
        assertEquals("第一问", messages.get(1).getText());
        assertEquals("assistant", messages.get(2).getMessageType().getValue());
        assertEquals("第一答", messages.get(2).getText());
        assertEquals("user", messages.get(3).getMessageType().getValue());
        assertEquals("第二问", messages.get(3).getText());

        Object options = prompt.getOptions();
        assertInstanceOf(OpenAiChatOptions.class, options);
        OpenAiChatOptions chatOptions = (OpenAiChatOptions) options;
        assertEquals("qwen-plus", chatOptions.getModel());
        assertEquals(1024, chatOptions.getMaxTokens());
        assertEquals(0.6D, chatOptions.getTemperature());

        assertEquals("qwen-plus", resp.getModelCode());
        assertEquals("最终回答", resp.getContent());
        assertEquals("推理内容", resp.getReasoningContent());
        assertEquals(12, resp.getTokenInput());
        assertEquals(23, resp.getTokenOutput());
        assertEquals(35, resp.getTokenTotal());
    }

    @Test
    void chatShouldMapProviderErrorCode() {
        SpringAiQwenGatewayService gatewayService = new SpringAiQwenGatewayService(chatModel);
        AiChatGatewayReq req = buildGatewayReq();
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401 unauthorized"));

        AiModelGatewayException exception = assertThrows(AiModelGatewayException.class, () -> gatewayService.chat(req));

        assertEquals(AiChatConstants.MODEL_AUTH_ERROR, exception.getModelErrorCode());
        assertEquals("401 unauthorized", exception.getMessage());
        assertEquals(403, exception.toServiceException().getCode());
    }

    @Test
    void chatStreamShouldForwardDeltaAndComplete() {
        SpringAiQwenGatewayService gatewayService = new SpringAiQwenGatewayService(chatModel);
        AiChatGatewayReq req = buildGatewayReq();
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                buildChatResponse("你", "想", null, null, null),
                buildChatResponse("好", "法", 4, 5, 9)
        ));
        RecordingListener listener = new RecordingListener();

        gatewayService.chatStream(req, listener);

        assertEquals(List.of("start:qwen-plus", "content:你", "reasoning:想", "content:好", "reasoning:法", "complete"),
                listener.events);
        assertEquals("你好", listener.completedResponse.getContent());
        assertEquals("想法", listener.completedResponse.getReasoningContent());
        assertEquals(9, listener.completedResponse.getTokenTotal());
    }

    private AiChatGatewayReq buildGatewayReq() {
        AiChatGatewayReq req = new AiChatGatewayReq();
        req.setModelCode("qwen-plus");
        req.setSystemPrompt("你是一个严谨的开发助手。");
        req.setTemperature(new BigDecimal("0.6"));
        req.setMaxTokens(1024);
        req.setMessages(List.of(
                new AiChatGatewayMessage("USER", "第一问"),
                new AiChatGatewayMessage("ASSISTANT", "第一答"),
                new AiChatGatewayMessage("USER", "第二问")
        ));
        return req;
    }

    private ChatResponse buildChatResponse(String text, String reasoningContent,
                                           Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content(text)
                .properties(reasoningContent != null ? Map.of("reasoning_content", reasoningContent) : Map.of())
                .build();
        Usage usage = new Usage() {
            @Override
            public Integer getPromptTokens() {
                return promptTokens;
            }

            @Override
            public Integer getCompletionTokens() {
                return completionTokens;
            }

            @Override
            public Integer getTotalTokens() {
                return totalTokens;
            }

            @Override
            public Object getNativeUsage() {
                return null;
            }
        };
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(new Generation(assistantMessage)), metadata);
    }

    private static final class RecordingListener implements AiStreamListener {

        private final List<String> events = new ArrayList<>();

        private AiChatGatewayResp completedResponse;

        @Override
        public void onStart(String modelCode) {
            events.add("start:" + modelCode);
        }

        @Override
        public void onContentDelta(String delta) {
            events.add("content:" + delta);
        }

        @Override
        public void onReasoningDelta(String delta) {
            events.add("reasoning:" + delta);
        }

        @Override
        public void onComplete(AiChatGatewayResp response) {
            completedResponse = response;
            events.add("complete");
        }

        @Override
        public void onError(String errorCode, String errorMessage, Throwable throwable) {
            events.add("error:" + errorCode);
        }
    }
}
