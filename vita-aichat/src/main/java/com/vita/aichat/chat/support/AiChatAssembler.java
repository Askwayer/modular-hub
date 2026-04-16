package com.vita.aichat.chat.support;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.chat.model.context.PreparedSendContext;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.vo.AiChatConversationDetailVo;
import com.vita.aichat.chat.vo.AiChatConversationPageVo;
import com.vita.aichat.chat.vo.AiChatMessageVo;
import com.vita.aichat.chat.vo.AiChatSendRespVo;
import com.vita.aichat.enums.AiRoleTypeEnum;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 负责正式 Chat 链路里的对象转换和 Prompt 消息组装。
 */
public final class AiChatAssembler {

    private AiChatAssembler() {
    }

    public static AiChatConversationPageVo toConversationPageVo(AiConversation conversation) {
        AiChatConversationPageVo vo = new AiChatConversationPageVo();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setTitleSource(conversation.getTitleSource());
        vo.setStatus(conversation.getStatus());
        vo.setPinned(conversation.getPinned());
        vo.setMessageCount(conversation.getMessageCount());
        vo.setRoundCount(conversation.getRoundCount());
        vo.setCurrentModelCode(conversation.getCurrentModelCode());
        vo.setSummaryStatus(conversation.getSummaryStatus());
        vo.setLastMessageAt(conversation.getLastMessageAt());
        vo.setCreateTime(conversation.getCreateTime());
        return vo;
    }

    public static AiChatConversationDetailVo toConversationDetailVo(AiConversation conversation) {
        AiChatConversationDetailVo vo = new AiChatConversationDetailVo();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setTitleSource(conversation.getTitleSource());
        vo.setStatus(conversation.getStatus());
        vo.setPinned(conversation.getPinned());
        vo.setMessageCount(conversation.getMessageCount());
        vo.setRoundCount(conversation.getRoundCount());
        vo.setCurrentModelCode(conversation.getCurrentModelCode());
        vo.setSystemPrompt(conversation.getSystemPrompt());
        vo.setTemperature(conversation.getTemperature());
        vo.setMaxTokens(conversation.getMaxTokens());
        vo.setMaxContextMessages(conversation.getMaxContextMessages());
        vo.setSummaryVersion(conversation.getSummaryVersion());
        vo.setSummaryStatus(conversation.getSummaryStatus());
        vo.setLastMessageAt(conversation.getLastMessageAt());
        vo.setCreateTime(conversation.getCreateTime());
        vo.setUpdateTime(conversation.getUpdateTime());
        return vo;
    }

    public static AiChatMessageVo toMessageVo(AiMessage message) {
        AiChatMessageVo vo = new AiChatMessageVo();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setRoleType(message.getRoleType());
        vo.setMessageType(message.getMessageType());
        vo.setReplyToId(message.getReplyToId());
        vo.setRequestId(message.getRequestId());
        vo.setModelCode(message.getModelCode());
        vo.setContent(message.getContent());
        vo.setReasoningContent(message.getReasoningContent());
        vo.setContentStatus(message.getContentStatus());
        vo.setUseContext(message.getUseContext());
        vo.setTokenInput(message.getTokenInput());
        vo.setTokenOutput(message.getTokenOutput());
        vo.setTokenTotal(message.getTokenTotal());
        vo.setErrorCode(message.getErrorCode());
        vo.setErrorMessage(message.getErrorMessage());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    public static AiChatSendRespVo toSendResp(PreparedSendContext context, AiChatCompletionResult response) {
        AiChatSendRespVo vo = new AiChatSendRespVo();
        vo.setConversationId(context.getConversation().getId());
        vo.setUserMessageId(context.getUserMessage().getId());
        vo.setAssistantMessageId(context.getAssistantMessage().getId());
        vo.setRequestId(context.getRequestId());
        vo.setContentStatus(context.getAssistantMessage().getContentStatus());
        vo.setModelCode(response.getModelCode());
        vo.setContent(response.getContent());
        vo.setReasoningContent(response.getReasoningContent());
        vo.setTokenInput(response.getTokenInput());
        vo.setTokenOutput(response.getTokenOutput());
        vo.setTokenTotal(response.getTokenTotal());
        return vo;
    }

    public static List<Message> buildPromptMessages(AiConversationSummary summary,
                                                    List<AiMessage> history,
                                                    AiMessage currentUserMessage) {
        List<Message> messages = new ArrayList<>();
        if (summary != null && StringUtils.hasText(summary.getSummaryText())) {
            messages.add(new SystemMessage("历史对话摘要:\n" + summary.getSummaryText()));
        }
        if (history != null && !history.isEmpty()) {
            List<AiMessage> orderedHistory = new ArrayList<>(history);
            Collections.reverse(orderedHistory);
            for (AiMessage item : orderedHistory) {
                if (!StringUtils.hasText(item.getContent())) {
                    continue;
                }
                messages.add(toPromptMessage(item.getRoleType(), item.getContent()));
            }
        }
        messages.add(new UserMessage(currentUserMessage.getContent()));
        return messages;
    }

    private static Message toPromptMessage(String roleType, String content) {
        if (AiRoleTypeEnum.SYSTEM.name().equalsIgnoreCase(roleType) || AiRoleTypeEnum.SUMMARY.name().equalsIgnoreCase(roleType)) {
            return new SystemMessage(content);
        }
        if (AiRoleTypeEnum.ASSISTANT.name().equalsIgnoreCase(roleType)) {
            return new AssistantMessage(content);
        }
        return new UserMessage(content);
    }
}
