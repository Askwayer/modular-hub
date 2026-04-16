package com.vita.aichat.chat.support;

import com.alibaba.fastjson2.JSON;
import com.vita.core.CommonStreamResult;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE 事件写出器。
 */
@Component
public class AiChatSseWriter {

    public SseEmitter createEmitter() {
        return new SseEmitter(0L);
    }

    public void sendSuccess(SseEmitter emitter, String event, String requestId, Object content) throws IOException {
        CommonStreamResult<Object> payload = CommonStreamResult.success(event, requestId, content);
        emitter.send(SseEmitter.event()
                .name(event)
                .data(JSON.toJSONString(payload), MediaType.APPLICATION_JSON));
    }

    public void sendError(SseEmitter emitter,
                          String requestId,
                          Integer code,
                          String message,
                          Object content) throws IOException {
        CommonStreamResult<Object> payload = CommonStreamResult.error("error", requestId, code, message, content);
        emitter.send(SseEmitter.event()
                .name("error")
                .data(JSON.toJSONString(payload), MediaType.APPLICATION_JSON));
    }
}
