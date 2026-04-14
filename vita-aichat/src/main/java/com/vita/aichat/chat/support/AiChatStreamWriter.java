package com.vita.aichat.chat.support;

import com.vita.aichat.gateway.AiChatGatewayResp;

/**
 * 屏蔽底层 SSE 实现，统一输出正式 Chat 事件。
 */
public interface AiChatStreamWriter {

    void sendStart(Long assistantMessageId, String requestId, String modelCode);

    void sendContentDelta(String delta);

    void sendReasoningDelta(String delta);

    void sendEnd(AiChatGatewayResp response);

    void sendError(String errorCode, String errorMessage);

    void complete();

    void completeWithError(Throwable throwable);
}
