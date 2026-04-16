package com.vita.aichat.chat.model.context;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.chat.dto.AiMessageSendDto;
import lombok.Data;

/**
 * 发送请求预留上下文。
 *
 * <p>用于在正式进入模型调用前完成轻校验与 requestId 防重预留。</p>
 */
@Data
public class ReservedSendContext {

    /* 用户ID */
    private Long userId;

    /* 会话ID */
    private Long conversationId;

    /* 请求唯一标识 */
    private String requestId;

    /* 会话快照 */
    private AiConversation conversation;

    /* 原始发送请求 */
    private AiMessageSendDto sendDto;
}
