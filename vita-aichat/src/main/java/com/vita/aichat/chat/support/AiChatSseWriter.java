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

    /**
     * 创建 SSE 连接。
     *
     * @return SSE 发射器
     */
    public SseEmitter createEmitter() {
        return new SseEmitter(0L);
    }

    /**
     * 发送成功事件。
     *
     * @param emitter SSE 发射器
     * @param event 事件名称
     * @param requestId 请求唯一标识
     * @param content 事件内容
     * @throws IOException 写出异常
     */
    public void sendSuccess(SseEmitter emitter, String event, String requestId, Object content) throws IOException {
        CommonStreamResult<Object> payload = CommonStreamResult.success(event, requestId, content);
        emitter.send(SseEmitter.event()
                .name(event)
                .data(JSON.toJSONString(payload), MediaType.APPLICATION_JSON));
    }

    /**
     * 发送错误事件。
     *
     * @param emitter SSE 发射器
     * @param requestId 请求唯一标识
     * @param code 错误码
     * @param message 错误信息
     * @param content 附加内容
     * @throws IOException 写出异常
     */
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
