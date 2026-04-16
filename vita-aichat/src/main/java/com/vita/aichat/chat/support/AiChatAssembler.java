package com.vita.aichat.chat.support;

import cn.hutool.core.bean.BeanUtil;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.aiMessage.entity.AiMessage;
import com.vita.aichat.chat.model.result.AiChatCompletionResult;
import com.vita.aichat.chat.vo.*;
import org.springframework.stereotype.Component;

/**
 * AI Chat 视图装配器。
 */
@Component
public class AiChatAssembler {

    public AiConversationPageVo toConversationPageVo(AiConversation conversation) {
        return BeanUtil.copyProperties(conversation, AiConversationPageVo.class);
    }

    public AiConversationDetailVo toConversationDetailVo(AiConversation conversation) {
        return BeanUtil.copyProperties(conversation, AiConversationDetailVo.class);
    }

    public AiMessagePageVo toMessagePageVo(AiMessage message) {
        return BeanUtil.copyProperties(message, AiMessagePageVo.class);
    }

    public AiChatSendVo toSendVo(AiMessage userMessage,
                                 AiMessage assistantMessage,
                                 AiChatCompletionResult completionResult) {
        AiChatSendVo vo = new AiChatSendVo();
        vo.setConversationId(assistantMessage.getConversationId());
        vo.setRequestId(assistantMessage.getRequestId());
        vo.setUserMessageId(userMessage.getId());
        vo.setAssistantMessageId(assistantMessage.getId());
        vo.setModelCode(assistantMessage.getModelCode());
        vo.setContent(completionResult.getContent());
        vo.setReasoningContent(completionResult.getReasoningContent());
        vo.setContentStatus(assistantMessage.getContentStatus());
        vo.setFinishReason(completionResult.getFinishReason());
        vo.setTokenInput(completionResult.getTokenInput());
        vo.setTokenOutput(completionResult.getTokenOutput());
        vo.setTokenTotal(completionResult.getTokenTotal());
        vo.setFirstTokenAt(completionResult.getFirstTokenAt());
        vo.setCompletedAt(completionResult.getCompletedAt());
        vo.setStreamBatchCount(completionResult.getStreamBatchCount());
        return vo;
    }

    public AiChatStreamEventVo toStreamEventVo(Long conversationId,
                                               Long assistantMessageId,
                                               String requestId,
                                               Integer batchNo) {
        AiChatStreamEventVo vo = new AiChatStreamEventVo();
        vo.setConversationId(conversationId);
        vo.setAssistantMessageId(assistantMessageId);
        vo.setRequestId(requestId);
        vo.setBatchNo(batchNo);
        return vo;
    }
}
