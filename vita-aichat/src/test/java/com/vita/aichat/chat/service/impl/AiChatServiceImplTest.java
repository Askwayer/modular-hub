package com.vita.aichat.chat.service.impl;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatProperties;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.support.AiChatStreamWriter;
import com.vita.aichat.chat.vo.AiChatSendRespVo;
import com.vita.aichat.config.AiChatProperties;
import com.vita.aichat.enums.AiContentStatusEnum;
import com.vita.aichat.enums.AiConversationStatusEnum;
import com.vita.aichat.enums.AiRoleTypeEnum;
import com.vita.aichat.enums.AiSummaryStatusEnum;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.ServiceException;
import com.vita.core.model.LoginUserInfoModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private IAiConversationService aiConversationService;

    @Mock
    private IAiMessageService aiMessageService;

    @Mock
    private IAiConversationSummaryService aiConversationSummaryService;

    @Mock
    private ChatModel chatModel;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        AiChatProperties aiChatProperties = new AiChatProperties();
        DashScopeChatProperties dashScopeChatProperties = new DashScopeChatProperties();
        dashScopeChatProperties.getOptions().setModel("qwen-plus");
        aiChatService = new AiChatServiceImpl(
                aiConversationService,
                aiMessageService,
                aiConversationSummaryService,
                chatModel,
                Runnable::run,
                aiChatProperties,
                dashScopeChatProperties
        );

        LoginUserInfoModel loginUserInfoModel = new LoginUserInfoModel();
        loginUserInfoModel.setId(100L);
        loginUserInfoModel.setUsername("coder");
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);
    }

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void sendMessageShouldUseDashScopeOptionsAndPersistReasoningContent() {
        AiConversation conversation = buildConversation("qwq-plus");
        AiConversationSummary summary = new AiConversationSummary();
        summary.setSummaryText("这是历史摘要");
        AiMessage historyUser = buildHistoryMessage(11L, AiRoleTypeEnum.USER.name(), "第一问");
        AiMessage historyAssistant = buildHistoryMessage(12L, AiRoleTypeEnum.ASSISTANT.name(), "第一答");

        when(aiConversationService.getByIdAndUserId(1L, 100L)).thenReturn(conversation);
        when(aiConversationSummaryService.getLatestReadySummary(1L, 100L)).thenReturn(summary);
        when(aiMessageService.listRecentContextMessages(eq(1L), eq(100L), anyLong(), anyInt()))
                .thenReturn(List.of(historyAssistant, historyUser));
        when(chatModel.call(any(Prompt.class))).thenReturn(buildChatResponse("最终回答", "推理内容", 12, 23, 35, "qwq-plus"));

        AiChatSendRespVo response = aiChatService.sendMessage(1L, buildSendReq("第二问"));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertThat(prompt.getInstructions()).hasSize(5);
        assertThat(prompt.getInstructions().get(0).getText()).isEqualTo("你是一个严谨的开发助手。");
        assertThat(prompt.getInstructions().get(1).getText()).contains("历史对话摘要");
        assertThat(prompt.getInstructions().get(4).getText()).isEqualTo("第二问");
        assertThat(prompt.getOptions()).isInstanceOf(DashScopeChatOptions.class);
        DashScopeChatOptions options = (DashScopeChatOptions) prompt.getOptions();
        assertThat(options.getModel()).isEqualTo("qwq-plus");
        assertThat(options.getEnableThinking()).isTrue();
        assertThat(options.getMaxTokens()).isEqualTo(2048);
        assertThat(options.getTemperature()).isEqualTo(0.6D);

        ArgumentCaptor<AiMessage> updateCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageService).update(updateCaptor.capture());
        AiMessage updatedAssistantMessage = updateCaptor.getValue();
        assertThat(updatedAssistantMessage.getContentStatus()).isEqualTo(AiContentStatusEnum.DONE.name());
        assertThat(updatedAssistantMessage.getContent()).isEqualTo("最终回答");
        assertThat(updatedAssistantMessage.getReasoningContent()).isEqualTo("推理内容");
        assertThat(updatedAssistantMessage.getTokenTotal()).isEqualTo(35);

        verify(aiMessageService, times(2)).create(any(AiMessage.class));
        assertThat(response.getModelCode()).isEqualTo("qwq-plus");
        assertThat(response.getReasoningContent()).isEqualTo("推理内容");
        assertThat(response.getTokenTotal()).isEqualTo(35);
    }

    @Test
    void doStreamMessageShouldEmitDeltaEventsAndPersistFinalResult() {
        PreparedSendContext context = new PreparedSendContext();
        AiConversation conversation = buildConversation("qwq-plus");
        AiMessage userMessage = buildHistoryMessage(21L, AiRoleTypeEnum.USER.name(), "再说一遍");
        userMessage.setRequestId("chat-req-001");
        AiMessage assistantMessage = buildHistoryMessage(22L, AiRoleTypeEnum.ASSISTANT.name(), "");
        assistantMessage.setContentStatus(AiContentStatusEnum.GENERATING.name());
        context.setConversation(conversation);
        context.setUserId(100L);
        context.setRequestId("chat-req-001");
        context.setUserMessage(userMessage);
        context.setAssistantMessage(assistantMessage);

        when(aiConversationSummaryService.getLatestReadySummary(1L, 100L)).thenReturn(null);
        when(aiMessageService.listRecentContextMessages(eq(1L), eq(100L), eq(21L), anyInt())).thenReturn(List.of());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                buildChatResponse("你", "想", null, null, null, "qwq-plus"),
                buildChatResponse("好", "法", 4, 5, 9, "qwq-plus")
        ));

        RecordingStreamWriter writer = new RecordingStreamWriter();
        aiChatService.doStreamMessage(context, writer);

        assertThat(writer.events).containsExactly(
                "start:qwq-plus",
                "content:你",
                "reasoning:想",
                "content:好",
                "reasoning:法",
                "end",
                "complete"
        );
        assertThat(writer.endResult.getContent()).isEqualTo("你好");
        assertThat(writer.endResult.getReasoningContent()).isEqualTo("想法");
        assertThat(writer.endResult.getTokenTotal()).isEqualTo(9);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(promptCaptor.capture());
        DashScopeChatOptions options = (DashScopeChatOptions) promptCaptor.getValue().getOptions();
        assertThat(options.getStream()).isTrue();
        assertThat(options.getIncrementalOutput()).isTrue();
        assertThat(options.getEnableThinking()).isTrue();

        ArgumentCaptor<AiMessage> updateCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageService).update(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getContentStatus()).isEqualTo(AiContentStatusEnum.DONE.name());
        assertThat(updateCaptor.getValue().getContent()).isEqualTo("你好");
    }

    @Test
    void sendMessageShouldMapUnauthorizedToAuthError() {
        AiConversation conversation = buildConversation("qwen-plus");
        when(aiConversationService.getByIdAndUserId(1L, 100L)).thenReturn(conversation);
        when(aiConversationSummaryService.getLatestReadySummary(1L, 100L)).thenReturn(null);
        when(aiMessageService.listRecentContextMessages(eq(1L), eq(100L), anyLong(), anyInt())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401 unauthorized"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> aiChatService.sendMessage(1L, buildSendReq("会失败的问题")));

        assertThat(exception.getCode()).isEqualTo(403);
        assertThat(exception.getMessage()).contains("模型鉴权失败");

        ArgumentCaptor<AiMessage> updateCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageService).update(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getContentStatus()).isEqualTo(AiContentStatusEnum.ERROR.name());
        assertThat(updateCaptor.getValue().getErrorCode()).isEqualTo("MODEL_AUTH_ERROR");
    }

    private AiConversation buildConversation(String modelCode) {
        AiConversation conversation = new AiConversation();
        conversation.setId(1L);
        conversation.setUserId(100L);
        conversation.setStatus(AiConversationStatusEnum.ACTIVE.name());
        conversation.setCurrentModelCode(modelCode);
        conversation.setSystemPrompt("你是一个严谨的开发助手。");
        conversation.setTemperature(new BigDecimal("0.6"));
        conversation.setMaxTokens(2048);
        conversation.setMaxContextMessages(12);
        conversation.setMessageCount(0);
        conversation.setRoundCount(0);
        conversation.setSummaryStatus(AiSummaryStatusEnum.NONE.name());
        return conversation;
    }

    private AiMessage buildHistoryMessage(Long id, String roleType, String content) {
        AiMessage message = new AiMessage();
        message.setId(id);
        message.setConversationId(1L);
        message.setUserId(100L);
        message.setRoleType(roleType);
        message.setContent(content);
        message.setContentStatus(AiContentStatusEnum.DONE.name());
        return message;
    }

    private com.vita.aichat.chat.dto.AiChatMessageSendReqDto buildSendReq(String content) {
        com.vita.aichat.chat.dto.AiChatMessageSendReqDto reqDto = new com.vita.aichat.chat.dto.AiChatMessageSendReqDto();
        reqDto.setContent(content);
        return reqDto;
    }

    private ChatResponse buildChatResponse(String text,
                                           String reasoningContent,
                                           Integer promptTokens,
                                           Integer completionTokens,
                                           Integer totalTokens,
                                           String modelCode) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content(text)
                .properties(reasoningContent != null ? Map.of("reasoningContent", reasoningContent) : Map.of())
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
        ChatResponseMetadata responseMetadata = ChatResponseMetadata.builder()
                .model(modelCode)
                .usage(usage)
                .build();
        ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
                .metadata("reasoningContent", reasoningContent)
                .build();
        return new ChatResponse(List.of(new Generation(assistantMessage, generationMetadata)), responseMetadata);
    }

    private static final class RecordingStreamWriter implements AiChatStreamWriter {

        private final List<String> events = new ArrayList<>();

        private AiChatCompletionResult endResult;

        @Override
        public void sendStart(Long assistantMessageId, String requestId, String modelCode) {
            events.add("start:" + modelCode);
        }

        @Override
        public void sendContentDelta(String delta) {
            events.add("content:" + delta);
        }

        @Override
        public void sendReasoningDelta(String delta) {
            events.add("reasoning:" + delta);
        }

        @Override
        public void sendEnd(AiChatCompletionResult response) {
            this.endResult = response;
            events.add("end");
        }

        @Override
        public void sendError(String errorCode, String errorMessage) {
            events.add("error:" + errorCode + ":" + errorMessage);
        }

        @Override
        public void complete() {
            events.add("complete");
        }

        @Override
        public void completeWithError(Throwable throwable) {
            events.add("complete-error");
        }
    }
}
