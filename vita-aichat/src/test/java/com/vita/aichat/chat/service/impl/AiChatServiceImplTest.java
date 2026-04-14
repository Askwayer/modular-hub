package com.vita.aichat.chat.service.impl;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.AiChatConstants;
import com.vita.aichat.chat.dto.AiChatConversationCreateReqDto;
import com.vita.aichat.chat.dto.AiChatConversationRenameReqDto;
import com.vita.aichat.chat.dto.AiChatMessageSendReqDto;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.support.AiChatStreamWriter;
import com.vita.aichat.chat.vo.AiChatSendRespVo;
import com.vita.aichat.config.AiChatProperties;
import com.vita.aichat.enums.*;
import com.vita.aichat.gateway.AiChatGatewayReq;
import com.vita.aichat.gateway.AiChatGatewayResp;
import com.vita.aichat.gateway.AiModelGatewayException;
import com.vita.aichat.gateway.AiModelGatewayService;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.model.LoginUserInfoModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    private static final Long LOGIN_USER_ID = 1001L;

    @Mock
    private IAiConversationService aiConversationService;
    @Mock
    private IAiMessageService aiMessageService;
    @Mock
    private IAiConversationSummaryService aiConversationSummaryService;
    @Mock
    private AiModelGatewayService aiModelGatewayService;

    private AiChatServiceImpl aiChatService;
    private AiChatProperties aiChatProperties;

    @BeforeEach
    void setUp() {
        aiChatProperties = new AiChatProperties();
        aiChatProperties.setDefaultModelCode("qwen-plus");
        aiChatProperties.setDefaultSystemPrompt("你是一个严谨的开发助手。");
        aiChatProperties.setMaxContextMessages(12);
        aiChatProperties.setReadTimeoutMs(60000L);
        aiChatProperties.setStreamEnabled(true);
        aiChatService = new AiChatServiceImpl(
                aiConversationService,
                aiMessageService,
                aiConversationSummaryService,
                aiModelGatewayService,
                aiChatProperties,
                Runnable::run
        );

        LoginUserInfoModel loginUser = new LoginUserInfoModel();
        loginUser.setId(LOGIN_USER_ID);
        loginUser.setUsername("tester");
        LoginUserInfoModelContext.setLoginUserInfo(loginUser);
    }

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void createConversationShouldFillDefaultsAndUseLoginUser() {
        AiChatConversationCreateReqDto reqDto = new AiChatConversationCreateReqDto();

        Long conversationId = aiChatService.createConversation(reqDto);

        ArgumentCaptor<AiConversation> captor = ArgumentCaptor.forClass(AiConversation.class);
        verify(aiConversationService).create(captor.capture());
        AiConversation inserted = captor.getValue();
        assertEquals(conversationId, inserted.getId());
        assertEquals(LOGIN_USER_ID, inserted.getUserId());
        assertEquals(AiChatConstants.DEFAULT_TITLE, inserted.getTitle());
        assertEquals(AiTitleSourceEnum.AUTO.name(), inserted.getTitleSource());
        assertEquals(AiConversationStatusEnum.ACTIVE.name(), inserted.getStatus());
        assertEquals(AiSummaryStatusEnum.NONE.name(), inserted.getSummaryStatus());
        assertEquals("qwen-plus", inserted.getCurrentModelCode());
        assertEquals("你是一个严谨的开发助手。", inserted.getSystemPrompt());
        assertEquals(12, inserted.getMaxContextMessages());
        assertNotNull(inserted.getId());
    }

    @Test
    void renameConversationShouldRejectMissingConversation() {
        when(aiConversationService.getByIdAndUserId(999L, LOGIN_USER_ID)).thenReturn(null);

        AiChatConversationRenameReqDto reqDto = new AiChatConversationRenameReqDto();
        reqDto.setTitle("新标题");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> aiChatService.renameConversation(999L, reqDto));
        assertEquals("会话不存在", exception.getMessage());
    }

    @Test
    void sendMessageShouldPersistUserAndAssistantAndUpdateConversationOnSuccess() {
        AiConversation conversation = buildConversation(2001L);
        when(aiConversationService.getByIdAndUserId(2001L, LOGIN_USER_ID)).thenReturn(conversation);
        when(aiConversationSummaryService.getLatestReadySummary(2001L, LOGIN_USER_ID))
                .thenReturn(buildSummary("这是历史摘要"));
        when(aiMessageService.listRecentContextMessages(eq(2001L), eq(LOGIN_USER_ID), anyLong(), eq(12)))
                .thenReturn(new ArrayList<>(List.of(
                        buildHistoryMessage(AiRoleTypeEnum.ASSISTANT.name(), "历史回复"),
                        buildHistoryMessage(AiRoleTypeEnum.USER.name(), "历史问题")
                )));
        AiChatGatewayResp gatewayResp = new AiChatGatewayResp();
        gatewayResp.setModelCode("qwen-plus");
        gatewayResp.setContent("新的回答");
        gatewayResp.setReasoningContent("推理过程");
        gatewayResp.setTokenInput(11);
        gatewayResp.setTokenOutput(22);
        gatewayResp.setTokenTotal(33);
        when(aiModelGatewayService.chat(any())).thenReturn(gatewayResp);
        List<AiMessage> insertedMessages = new ArrayList<>();
        doAnswer(invocation -> {
            insertedMessages.add(copyMessage(invocation.getArgument(0)));
            return 1;
        }).when(aiMessageService).create(any(AiMessage.class));

        AiChatMessageSendReqDto reqDto = new AiChatMessageSendReqDto();
        reqDto.setContent("请总结一下");

        AiChatSendRespVo respVo = aiChatService.sendMessage(2001L, reqDto);

        verify(aiMessageService, org.mockito.Mockito.times(2)).create(any(AiMessage.class));
        AiMessage userMessage = insertedMessages.get(0);
        AiMessage assistantMessage = insertedMessages.get(1);
        assertEquals(AiRoleTypeEnum.USER.name(), userMessage.getRoleType());
        assertEquals("请总结一下", userMessage.getContent());
        assertEquals(AiContentStatusEnum.DONE.name(), userMessage.getContentStatus());
        assertEquals(AiRoleTypeEnum.ASSISTANT.name(), assistantMessage.getRoleType());
        assertEquals(AiContentStatusEnum.GENERATING.name(), assistantMessage.getContentStatus());
        assertEquals(userMessage.getId(), assistantMessage.getReplyToId());

        ArgumentCaptor<AiChatGatewayReq> gatewayReqCaptor = ArgumentCaptor.forClass(AiChatGatewayReq.class);
        verify(aiModelGatewayService).chat(gatewayReqCaptor.capture());
        AiChatGatewayReq gatewayReq = gatewayReqCaptor.getValue();
        assertEquals("qwen-plus", gatewayReq.getModelCode());
        assertEquals("你是一个严谨的开发助手。", gatewayReq.getSystemPrompt());
        assertEquals(4, gatewayReq.getMessages().size());
        assertEquals("历史对话摘要:\n这是历史摘要", gatewayReq.getMessages().get(0).getContent());
        assertEquals(AiRoleTypeEnum.USER.name(), gatewayReq.getMessages().get(1).getRole());
        assertEquals("历史问题", gatewayReq.getMessages().get(1).getContent());
        assertEquals(AiRoleTypeEnum.ASSISTANT.name(), gatewayReq.getMessages().get(2).getRole());
        assertEquals("历史回复", gatewayReq.getMessages().get(2).getContent());
        assertEquals("请总结一下", gatewayReq.getMessages().get(3).getContent());

        ArgumentCaptor<AiMessage> updateCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageService).update(updateCaptor.capture());
        AiMessage updatedAssistant = updateCaptor.getValue();
        assertEquals(AiContentStatusEnum.DONE.name(), updatedAssistant.getContentStatus());
        assertEquals("新的回答", updatedAssistant.getContent());
        assertEquals("推理过程", updatedAssistant.getReasoningContent());
        assertEquals(33, updatedAssistant.getTokenTotal());

        ArgumentCaptor<AiConversation> conversationCaptor = ArgumentCaptor.forClass(AiConversation.class);
        verify(aiConversationService).update(conversationCaptor.capture());
        AiConversation updatedConversation = conversationCaptor.getValue();
        assertEquals(2, updatedConversation.getMessageCount());
        assertEquals(1, updatedConversation.getRoundCount());
        assertEquals("qwen-plus", updatedConversation.getCurrentModelCode());

        assertEquals(conversation.getId(), respVo.getConversationId());
        assertEquals(updatedAssistant.getId(), respVo.getAssistantMessageId());
        assertEquals("新的回答", respVo.getContent());
        assertEquals("推理过程", respVo.getReasoningContent());
        assertEquals(33, respVo.getTokenTotal());
    }

    @Test
    void sendMessageShouldMarkAssistantErrorWithTypedGatewayCode() {
        AiConversation conversation = buildConversation(2002L);
        when(aiConversationService.getByIdAndUserId(2002L, LOGIN_USER_ID)).thenReturn(conversation);
        when(aiConversationSummaryService.getLatestReadySummary(2002L, LOGIN_USER_ID)).thenReturn(null);
        when(aiMessageService.listRecentContextMessages(eq(2002L), eq(LOGIN_USER_ID), anyLong(), eq(12)))
                .thenReturn(new ArrayList<>());
        when(aiModelGatewayService.chat(any())).thenThrow(
                new AiModelGatewayException(
                        AiChatConstants.MODEL_TIMEOUT,
                        "模型调用超时",
                        new ServiceException(GlobalErrorCode.GATEWAY_TIMEOUT).setMessage("模型调用超时")
                )
        );

        AiChatMessageSendReqDto reqDto = new AiChatMessageSendReqDto();
        reqDto.setContent("继续");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> aiChatService.sendMessage(2002L, reqDto));
        assertEquals("模型调用超时", exception.getMessage());

        ArgumentCaptor<AiMessage> updateCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageService).update(updateCaptor.capture());
        AiMessage updatedAssistant = updateCaptor.getValue();
        assertEquals(AiContentStatusEnum.ERROR.name(), updatedAssistant.getContentStatus());
        assertEquals(AiChatConstants.MODEL_TIMEOUT, updatedAssistant.getErrorCode());
        assertEquals("模型调用超时", updatedAssistant.getErrorMessage());
    }

    @Test
    void doStreamMessageShouldEmitEventsAndFinalizeAssistantMessage() {
        AiConversation conversation = buildConversation(2003L);
        when(aiConversationService.getByIdAndUserId(2003L, LOGIN_USER_ID)).thenReturn(conversation);
        when(aiConversationSummaryService.getLatestReadySummary(2003L, LOGIN_USER_ID)).thenReturn(null);
        when(aiMessageService.listRecentContextMessages(eq(2003L), eq(LOGIN_USER_ID), anyLong(), eq(12)))
                .thenReturn(new ArrayList<>());
        doAnswer(invocation -> {
            AiStreamListenerArgument listenerArgument = new AiStreamListenerArgument(invocation.getArgument(1));
            listenerArgument.listener.onStart("qwen-plus");
            listenerArgument.listener.onContentDelta("分段");
            listenerArgument.listener.onReasoningDelta("思考");
            AiChatGatewayResp response = new AiChatGatewayResp();
            response.setModelCode("qwen-plus");
            response.setContent("分段完成");
            response.setReasoningContent("思考完成");
            response.setTokenInput(7);
            response.setTokenOutput(8);
            response.setTokenTotal(15);
            listenerArgument.listener.onComplete(response);
            return null;
        }).when(aiModelGatewayService).chatStream(any(), any());

        AiChatMessageSendReqDto reqDto = new AiChatMessageSendReqDto();
        reqDto.setContent("流式回答");
        PreparedSendContext context = aiChatService.prepareSend(2003L, reqDto, LOGIN_USER_ID);
        RecordingStreamWriter writer = new RecordingStreamWriter();

        aiChatService.doStreamMessage(context, writer);

        assertEquals(List.of(
                AiChatConstants.EVENT_MESSAGE_START,
                AiChatConstants.EVENT_MESSAGE_DELTA,
                AiChatConstants.EVENT_REASONING_DELTA,
                AiChatConstants.EVENT_MESSAGE_END,
                "complete"
        ), writer.events);
        assertFalse(writer.completedWithError);

        ArgumentCaptor<AiMessage> updateCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageService).update(updateCaptor.capture());
        AiMessage updatedAssistant = updateCaptor.getValue();
        assertEquals(AiContentStatusEnum.DONE.name(), updatedAssistant.getContentStatus());
        assertEquals("分段完成", updatedAssistant.getContent());
        assertEquals("思考完成", updatedAssistant.getReasoningContent());
    }

    @Test
    void doStreamMessageShouldEmitErrorOnceAndMarkAssistantMessage() {
        AiConversation conversation = buildConversation(2004L);
        when(aiConversationService.getByIdAndUserId(2004L, LOGIN_USER_ID)).thenReturn(conversation);
        when(aiConversationSummaryService.getLatestReadySummary(2004L, LOGIN_USER_ID)).thenReturn(null);
        when(aiMessageService.listRecentContextMessages(eq(2004L), eq(LOGIN_USER_ID), anyLong(), eq(12)))
                .thenReturn(new ArrayList<>());
        doAnswer(invocation -> {
            AiStreamListenerArgument listenerArgument = new AiStreamListenerArgument(invocation.getArgument(1));
            listenerArgument.listener.onStart("qwen-plus");
            listenerArgument.listener.onError(AiChatConstants.MODEL_TIMEOUT, "请求超时", new RuntimeException("请求超时"));
            return null;
        }).when(aiModelGatewayService).chatStream(any(), any());

        AiChatMessageSendReqDto reqDto = new AiChatMessageSendReqDto();
        reqDto.setContent("流式异常");
        PreparedSendContext context = aiChatService.prepareSend(2004L, reqDto, LOGIN_USER_ID);
        RecordingStreamWriter writer = new RecordingStreamWriter();

        aiChatService.doStreamMessage(context, writer);

        assertEquals(List.of(
                AiChatConstants.EVENT_MESSAGE_START,
                AiChatConstants.EVENT_ERROR
        ), writer.events);
        assertTrue(writer.completedWithError);
        assertEquals(AiChatConstants.MODEL_TIMEOUT, writer.errorCode);
        assertEquals("请求超时", writer.errorMessage);

        ArgumentCaptor<AiMessage> updateCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageService).update(updateCaptor.capture());
        AiMessage updatedAssistant = updateCaptor.getValue();
        assertEquals(AiContentStatusEnum.ERROR.name(), updatedAssistant.getContentStatus());
        assertEquals(AiChatConstants.MODEL_TIMEOUT, updatedAssistant.getErrorCode());
    }

    private AiConversation buildConversation(Long conversationId) {
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);
        conversation.setUserId(LOGIN_USER_ID);
        conversation.setTitle("测试会话");
        conversation.setTitleSource(AiTitleSourceEnum.AUTO.name());
        conversation.setStatus(AiConversationStatusEnum.ACTIVE.name());
        conversation.setPinned((byte) 0);
        conversation.setMessageCount(0);
        conversation.setRoundCount(0);
        conversation.setCurrentModelCode(null);
        conversation.setSystemPrompt(null);
        conversation.setTemperature(BigDecimal.ONE);
        conversation.setMaxTokens(2048);
        conversation.setMaxContextMessages(null);
        conversation.setSummaryStatus(AiSummaryStatusEnum.NONE.name());
        return conversation;
    }

    private AiConversationSummary buildSummary(String summaryText) {
        AiConversationSummary summary = new AiConversationSummary();
        summary.setConversationId(1L);
        summary.setUserId(LOGIN_USER_ID);
        summary.setSummaryText(summaryText);
        summary.setStatus(AiSummaryStatusEnum.READY.name());
        return summary;
    }

    private AiMessage buildHistoryMessage(String roleType, String content) {
        AiMessage message = new AiMessage();
        message.setId(System.nanoTime());
        message.setConversationId(1L);
        message.setUserId(LOGIN_USER_ID);
        message.setRoleType(roleType);
        message.setContent(content);
        message.setContentStatus(AiContentStatusEnum.DONE.name());
        message.setUseContext((byte) 1);
        return message;
    }

    private AiMessage copyMessage(AiMessage source) {
        AiMessage target = new AiMessage();
        target.setId(source.getId());
        target.setConversationId(source.getConversationId());
        target.setUserId(source.getUserId());
        target.setRoleType(source.getRoleType());
        target.setMessageType(source.getMessageType());
        target.setReplyToId(source.getReplyToId());
        target.setRequestId(source.getRequestId());
        target.setModelId(source.getModelId());
        target.setModelCode(source.getModelCode());
        target.setContent(source.getContent());
        target.setReasoningContent(source.getReasoningContent());
        target.setContentStatus(source.getContentStatus());
        target.setUseContext(source.getUseContext());
        target.setTokenInput(source.getTokenInput());
        target.setTokenOutput(source.getTokenOutput());
        target.setTokenTotal(source.getTokenTotal());
        target.setErrorCode(source.getErrorCode());
        target.setErrorMessage(source.getErrorMessage());
        return target;
    }

    private static final class RecordingStreamWriter implements AiChatStreamWriter {

        private final List<String> events = new ArrayList<>();

        private boolean completedWithError;

        private String errorCode;

        private String errorMessage;

        @Override
        public void sendStart(Long assistantMessageId, String requestId, String modelCode) {
            events.add(AiChatConstants.EVENT_MESSAGE_START);
        }

        @Override
        public void sendContentDelta(String delta) {
            events.add(AiChatConstants.EVENT_MESSAGE_DELTA);
        }

        @Override
        public void sendReasoningDelta(String delta) {
            events.add(AiChatConstants.EVENT_REASONING_DELTA);
        }

        @Override
        public void sendEnd(AiChatGatewayResp response) {
            events.add(AiChatConstants.EVENT_MESSAGE_END);
        }

        @Override
        public void sendError(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            events.add(AiChatConstants.EVENT_ERROR);
        }

        @Override
        public void complete() {
            events.add("complete");
        }

        @Override
        public void completeWithError(Throwable throwable) {
            completedWithError = true;
        }
    }

    private record AiStreamListenerArgument(com.vita.aichat.gateway.AiStreamListener listener) {
    }
}
