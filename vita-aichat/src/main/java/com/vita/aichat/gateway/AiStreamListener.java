package com.vita.aichat.gateway;

/**
 * AI 流式监听器。
 */
public interface AiStreamListener {

    void onStart(String modelCode);

    void onContentDelta(String delta);

    void onReasoningDelta(String delta);

    void onComplete(AiChatGatewayResp response);

    void onError(String errorCode, String errorMessage, Throwable throwable);
}
