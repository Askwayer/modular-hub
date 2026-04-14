package com.vita.aichat.chat.support;

import com.vita.aichat.chat.AiChatConstants;
import com.vita.aichat.gateway.AiChatGatewayResp;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 基于 {@link SseEmitter} 输出正式 Chat 的 SSE 事件。
 */
public class SseEmitterAiChatStreamWriter implements AiChatStreamWriter {

    private final SseEmitter emitter;

    public SseEmitterAiChatStreamWriter(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void sendStart(Long assistantMessageId, String requestId, String modelCode) {
        send(AiChatConstants.EVENT_MESSAGE_START, Map.of(
                "assistantMessageId", assistantMessageId,
                "requestId", requestId,
                "modelCode", modelCode
        ));
    }

    @Override
    public void sendContentDelta(String delta) {
        send(AiChatConstants.EVENT_MESSAGE_DELTA, delta);
    }

    @Override
    public void sendReasoningDelta(String delta) {
        send(AiChatConstants.EVENT_REASONING_DELTA, delta);
    }

    @Override
    public void sendEnd(AiChatGatewayResp response) {
        send(AiChatConstants.EVENT_MESSAGE_END, response);
    }

    @Override
    public void sendError(String errorCode, String errorMessage) {
        send(AiChatConstants.EVENT_ERROR, Map.of(
                "errorCode", errorCode,
                "errorMessage", errorMessage
        ));
    }

    @Override
    public void complete() {
        emitter.complete();
    }

    @Override
    public void completeWithError(Throwable throwable) {
        emitter.completeWithError(throwable);
    }

    private void send(String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ex) {
            throw new IllegalStateException("SSE 事件发送失败", ex);
        }
    }
}
