package com.vita.aichat.chat.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.chat.AiChatConstants;
import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.support.AiChatAssembler;
import com.vita.aichat.chat.support.AiChatStreamWriter;
import com.vita.aichat.chat.support.SseEmitterAiChatStreamWriter;
import com.vita.aichat.chat.vo.AiChatConversationDetailVo;
import com.vita.aichat.chat.vo.AiChatConversationPageVo;
import com.vita.aichat.chat.vo.AiChatMessageVo;
import com.vita.aichat.chat.vo.AiChatSendRespVo;
import com.vita.aichat.config.AiChatProperties;
import com.vita.aichat.enums.*;
import com.vita.aichat.gateway.*;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.utils.SnowUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 编排正式 Chat 的会话管理、消息落库和模型调用流程。
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private final IAiConversationService aiConversationService;
    private final IAiMessageService aiMessageService;
    private final IAiConversationSummaryService aiConversationSummaryService;
    private final AiModelGatewayService aiModelGatewayService;
    private final AiChatProperties aiChatProperties;
    private final Executor aiChatExecutor;

    public AiChatServiceImpl(IAiConversationService aiConversationService,
                             IAiMessageService aiMessageService,
                             IAiConversationSummaryService aiConversationSummaryService,
                             AiModelGatewayService aiModelGatewayService,
                             AiChatProperties aiChatProperties,
                             @Qualifier("aiChatExecutor") Executor aiChatExecutor) {
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiModelGatewayService = aiModelGatewayService;
        this.aiChatProperties = aiChatProperties;
        this.aiChatExecutor = aiChatExecutor;
    }

    /**
     * 为当前登录用户创建一条可立即对话的会话记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(AiChatConversationCreateReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = new AiConversation();
        conversation.setId(SnowUtils.getSnowflakeNextId());
        conversation.setUserId(userId);
        conversation.setTitle(CharSequenceUtil.blankToDefault(reqDto.getTitle(), AiChatConstants.DEFAULT_TITLE));
        conversation.setTitleSource(AiTitleSourceEnum.AUTO.name());
        conversation.setStatus(AiConversationStatusEnum.ACTIVE.name());
        conversation.setPinned((byte) 0);
        conversation.setMessageCount(0);
        conversation.setRoundCount(0);
        conversation.setCurrentModelCode(defaultModelCode(reqDto.getModelCode()));
        conversation.setSystemPrompt(defaultSystemPrompt(reqDto.getSystemPrompt()));
        conversation.setTemperature(reqDto.getTemperature());
        conversation.setMaxTokens(reqDto.getMaxTokens());
        conversation.setMaxContextMessages(defaultMaxContextMessages(reqDto.getMaxContextMessages()));
        conversation.setSummaryVersion(0);
        conversation.setSummaryStatus(AiSummaryStatusEnum.NONE.name());
        aiConversationService.create(conversation);
        return conversation.getId();
    }

    @Override
    public PageResponse<AiChatConversationPageVo> pageConversations(AiChatConversationPageReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PageResponse<AiConversation> page = aiConversationService.pageByUserId(reqDto, userId, reqDto.getKeyword());
        List<AiChatConversationPageVo> list = new ArrayList<>();
        for (AiConversation item : page.getList()) {
            list.add(AiChatAssembler.toConversationPageVo(item));
        }
        return new PageResponse<>(list, page.getTotal());
    }

    @Override
    public AiChatConversationDetailVo getConversation(Long conversationId) {
        return AiChatAssembler.toConversationDetailVo(
                requireConversation(conversationId, LoginUserInfoModelContext.getRequiredLoginUserId())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameConversation(Long conversationId, AiChatConversationRenameReqDto reqDto) {
        AiConversation conversation = requireConversation(conversationId, LoginUserInfoModelContext.getRequiredLoginUserId());
        conversation.setTitle(reqDto.getTitle().trim());
        conversation.setTitleSource(AiTitleSourceEnum.MANUAL.name());
        aiConversationService.update(conversation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        AiConversation conversation = requireConversation(conversationId, userId);
        aiConversationService.deleteById(conversation.getId());
        aiMessageService.deleteByConversationIdAndUserId(conversationId, userId);
        aiConversationSummaryService.deleteByConversationIdAndUserId(conversationId, userId);
    }

    @Override
    public PageResponse<AiChatMessageVo> pageMessages(Long conversationId, AiChatMessagePageReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        requireConversation(conversationId, userId);
        PageResponse<AiMessage> page = aiMessageService.pageByConversationIdAndUserId(reqDto, conversationId, userId);
        List<AiChatMessageVo> list = new ArrayList<>();
        for (AiMessage item : page.getList()) {
            list.add(AiChatAssembler.toMessageVo(item));
        }
        return new PageResponse<>(list, page.getTotal());
    }

    /**
     * 写入用户消息、助手占位消息，再同步调用模型返回完整回复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSendRespVo sendMessage(Long conversationId, AiChatMessageSendReqDto reqDto) {
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PreparedSendContext context = prepareSend(conversationId, reqDto, userId);
        try {
            AiChatGatewayResp gatewayResp = aiModelGatewayService.chat(buildGatewayReq(context));
            handleGatewaySuccess(context, gatewayResp);
            return AiChatAssembler.toSendResp(context, gatewayResp, defaultModelCode(gatewayResp.getModelCode()));
        } catch (AiModelGatewayException ex) {
            handleGatewayFailure(context, ex.getModelErrorCode(), ex.getMessage());
            throw ex.toServiceException();
        } catch (ServiceException ex) {
            handleGatewayFailure(context, AiChatConstants.MODEL_PROVIDER_ERROR, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            handleGatewayFailure(context, AiChatConstants.MODEL_PROVIDER_ERROR, ex.getMessage());
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE).setMessage("模型调用失败");
        }
    }

    /**
     * 写入会话消息后启动异步模型调用，并通过 SSE 向客户端推送增量内容。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter streamMessage(Long conversationId, AiChatMessageSendReqDto reqDto) {
        if (!Boolean.TRUE.equals(aiChatProperties.getStreamEnabled())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST).setMessage("当前环境未开启流式回复");
        }
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        PreparedSendContext context = prepareSend(conversationId, reqDto, userId);
        SseEmitter emitter = new SseEmitter(aiChatProperties.getReadTimeoutMs());
        AiChatStreamWriter writer = new SseEmitterAiChatStreamWriter(emitter);
        aiChatExecutor.execute(() -> doStreamMessage(context, writer));
        return emitter;
    }

    /**
     * 执行真实的流式模型调用，并把生命周期事件写回消息表和 SSE。
     */
    void doStreamMessage(PreparedSendContext context, AiChatStreamWriter writer) {
        try {
            aiModelGatewayService.chatStream(buildGatewayReq(context), new AiStreamListener() {
                @Override
                public void onStart(String modelCode) {
                    writer.sendStart(context.getAssistantMessage().getId(), context.getRequestId(), modelCode);
                }

                @Override
                public void onContentDelta(String delta) {
                    writer.sendContentDelta(delta);
                }

                @Override
                public void onReasoningDelta(String delta) {
                    writer.sendReasoningDelta(delta);
                }

                @Override
                public void onComplete(AiChatGatewayResp response) {
                    handleGatewaySuccess(context, response);
                    writer.sendEnd(response);
                    writer.complete();
                }

                @Override
                public void onError(String errorCode, String errorMessage, Throwable throwable) {
                    handleGatewayFailure(context, errorCode, errorMessage);
                    writer.sendError(errorCode, errorMessage);
                    writer.completeWithError(throwable != null ? throwable : new IllegalStateException(errorMessage));
                }
            });
        } catch (Exception ex) {
            handleGatewayFailure(context, AiChatConstants.MODEL_PROVIDER_ERROR, ex.getMessage());
            writer.sendError(AiChatConstants.MODEL_PROVIDER_ERROR, normalizeErrorMessage(ex.getMessage()));
            writer.completeWithError(ex);
        }
    }

    /**
     * 校验会话归属并落库用户消息、助手占位消息，为后续模型调用建立上下文。
     */
    PreparedSendContext prepareSend(Long conversationId, AiChatMessageSendReqDto reqDto, Long userId) {
        validateMessageType(reqDto.getMessageType());
        AiConversation conversation = requireConversation(conversationId, userId);
        if (!AiConversationStatusEnum.ACTIVE.name().equals(conversation.getStatus())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST).setMessage("当前会话不可发送消息");
        }
        AiMessage userMessage = buildUserMessage(conversation, reqDto, userId);
        aiMessageService.create(userMessage);
        AiMessage assistantMessage = buildAssistantPlaceholder(conversation, userMessage, userId);
        aiMessageService.create(assistantMessage);

        PreparedSendContext context = new PreparedSendContext();
        context.setConversation(conversation);
        context.setUserMessage(userMessage);
        context.setAssistantMessage(assistantMessage);
        context.setUserId(userId);
        context.setRequestId(userMessage.getRequestId());
        return context;
    }

    private AiConversation requireConversation(Long conversationId, Long userId) {
        if (conversationId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AiConversation conversation = aiConversationService.getByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND).setMessage("会话不存在");
        }
        return conversation;
    }

    private AiMessage buildUserMessage(AiConversation conversation, AiChatMessageSendReqDto reqDto, Long userId) {
        AiMessage message = new AiMessage();
        message.setId(SnowUtils.getSnowflakeNextId());
        message.setConversationId(conversation.getId());
        message.setUserId(userId);
        message.setRoleType(AiRoleTypeEnum.USER.name());
        message.setMessageType(defaultMessageType(reqDto.getMessageType()));
        message.setRequestId(defaultRequestId(reqDto.getRequestId()));
        message.setModelCode(defaultModelCode(conversation.getCurrentModelCode()));
        message.setContent(reqDto.getContent().trim());
        message.setContentStatus(AiContentStatusEnum.DONE.name());
        message.setUseContext(defaultUseContext(reqDto.getUseContext()));
        return message;
    }

    private AiMessage buildAssistantPlaceholder(AiConversation conversation, AiMessage userMessage, Long userId) {
        AiMessage message = new AiMessage();
        message.setId(SnowUtils.getSnowflakeNextId());
        message.setConversationId(conversation.getId());
        message.setUserId(userId);
        message.setRoleType(AiRoleTypeEnum.ASSISTANT.name());
        message.setMessageType(AiChatConstants.MESSAGE_TYPE_TEXT);
        message.setReplyToId(userMessage.getId());
        message.setRequestId(userMessage.getRequestId());
        message.setModelCode(defaultModelCode(conversation.getCurrentModelCode()));
        message.setContentStatus(AiContentStatusEnum.GENERATING.name());
        message.setUseContext((byte) 1);
        message.setContent("");
        return message;
    }

    /**
     * 读取摘要和上下文消息，组装成统一网关请求。
     */
    private AiChatGatewayReq buildGatewayReq(PreparedSendContext context) {
        AiConversation conversation = context.getConversation();
        AiConversationSummary summary = aiConversationSummaryService
                .getLatestReadySummary(conversation.getId(), context.getUserId());
        List<AiMessage> history = aiMessageService.listRecentContextMessages(
                conversation.getId(),
                context.getUserId(),
                context.getUserMessage().getId(),
                defaultMaxContextMessages(conversation.getMaxContextMessages())
        );

        AiChatGatewayReq req = new AiChatGatewayReq();
        req.setModelCode(defaultModelCode(conversation.getCurrentModelCode()));
        req.setSystemPrompt(defaultSystemPrompt(conversation.getSystemPrompt()));
        req.setTemperature(conversation.getTemperature());
        req.setMaxTokens(conversation.getMaxTokens());
        req.setUserId(context.getUserId());
        req.setMessages(AiChatAssembler.buildGatewayMessages(summary, history, context.getUserMessage()));
        return req;
    }

    /**
     * 模型回复成功后更新助手消息状态，并回写会话统计。
     */
    private void handleGatewaySuccess(PreparedSendContext context, AiChatGatewayResp response) {
        AiMessage assistantMessage = context.getAssistantMessage();
        assistantMessage.setContent(defaultText(response.getContent()));
        assistantMessage.setReasoningContent(response.getReasoningContent());
        assistantMessage.setModelCode(defaultModelCode(response.getModelCode()));
        assistantMessage.setContentStatus(AiContentStatusEnum.DONE.name());
        assistantMessage.setTokenInput(response.getTokenInput());
        assistantMessage.setTokenOutput(response.getTokenOutput());
        assistantMessage.setTokenTotal(response.getTokenTotal());
        assistantMessage.setErrorCode(null);
        assistantMessage.setErrorMessage(null);
        aiMessageService.update(assistantMessage);
        updateConversationAfterReply(context.getConversation(), assistantMessage);
    }

    /**
     * 模型回复失败时标记助手消息错误状态，并保留错误码和错误摘要。
     */
    private void handleGatewayFailure(PreparedSendContext context, String errorCode, String errorMessage) {
        AiMessage assistantMessage = context.getAssistantMessage();
        assistantMessage.setContentStatus(AiContentStatusEnum.ERROR.name());
        assistantMessage.setErrorCode(errorCode);
        assistantMessage.setErrorMessage(normalizeErrorMessage(errorMessage));
        aiMessageService.update(assistantMessage);
        updateConversationAfterReply(context.getConversation(), assistantMessage);
    }

    private void updateConversationAfterReply(AiConversation conversation, AiMessage assistantMessage) {
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setLastMessageAt(LocalDateTime.now());
        update.setMessageCount((conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + 2);
        update.setRoundCount((conversation.getRoundCount() == null ? 0 : conversation.getRoundCount()) + 1);
        update.setCurrentModelCode(defaultModelCode(assistantMessage.getModelCode()));
        aiConversationService.update(update);
        conversation.setMessageCount(update.getMessageCount());
        conversation.setRoundCount(update.getRoundCount());
        conversation.setLastMessageAt(update.getLastMessageAt());
        conversation.setCurrentModelCode(update.getCurrentModelCode());
    }

    private void validateMessageType(String messageType) {
        String actualMessageType = defaultMessageType(messageType);
        if (!AiChatConstants.MESSAGE_TYPE_TEXT.equals(actualMessageType)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST).setMessage("当前仅支持文本消息");
        }
    }

    private String defaultModelCode(String modelCode) {
        return CharSequenceUtil.blankToDefault(modelCode, aiChatProperties.getDefaultModelCode());
    }

    private String defaultSystemPrompt(String systemPrompt) {
        return CharSequenceUtil.blankToDefault(systemPrompt, aiChatProperties.getDefaultSystemPrompt());
    }

    private Integer defaultMaxContextMessages(Integer maxContextMessages) {
        return maxContextMessages != null ? maxContextMessages : aiChatProperties.getMaxContextMessages();
    }

    private Byte defaultUseContext(Byte useContext) {
        return useContext != null ? useContext : (byte) 1;
    }

    private String defaultRequestId(String requestId) {
        return CharSequenceUtil.blankToDefault(requestId, "chat-" + UUID.randomUUID().toString().replace("-", ""));
    }

    private String defaultMessageType(String messageType) {
        return CharSequenceUtil.blankToDefault(messageType, AiChatConstants.MESSAGE_TYPE_TEXT);
    }

    private String normalizeErrorMessage(String errorMessage) {
        return CharSequenceUtil.blankToDefault(errorMessage, "模型调用失败");
    }

    private String defaultText(String value) {
        return value != null ? value : "";
    }
}
