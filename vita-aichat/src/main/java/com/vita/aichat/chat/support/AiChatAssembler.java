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

    /**
     * 转换为会话分页视图对象。
     *
     * @param conversation 会话实体
     * @return 会话分页视图
     */
    public AiConversationPageVo toConversationPageVo(AiConversation conversation) {
        return BeanUtil.copyProperties(conversation, AiConversationPageVo.class);
    }

    /**
     * 转换为会话详情视图对象。
     *
     * @param conversation 会话实体
     * @return 会话详情视图
     */
    public AiConversationDetailVo toConversationDetailVo(AiConversation conversation) {
        return BeanUtil.copyProperties(conversation, AiConversationDetailVo.class);
    }

    /**
     * 转换为消息分页视图对象。
     *
     * @param message 消息实体
     * @return 消息分页视图
     */
    public AiMessagePageVo toMessagePageVo(AiMessage message) {
        return BeanUtil.copyProperties(message, AiMessagePageVo.class);
    }

    /**
     * 转换为消息发送视图对象。
     *
     * @param userMessage      用户消息实体
     * @param assistantMessage 助手消息实体
     * @param completionResult 模型完成结果
     * @return 消息发送视图
     */
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

    /**
     * 转换为流式事件视图对象。
     *
     * @param conversationId     会话ID
     * @param assistantMessageId 助手消息ID
     * @param requestId          请求唯一标识
     * @param batchNo            批次号
     * @return 流式事件视图
     */
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
