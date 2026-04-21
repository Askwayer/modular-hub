package com.vita.aichat.chat.model.request;

import com.vita.aichat.chat.model.context.ReservedSendContext;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.aichat.chat.model.request
 * @Author: znk
 * @CreateTime: 2026-04-21  00:04:22
 * @Description: 流式请求状态跟踪对象
 * @Version: 1.0
 */
@Data
public class StreamRequestState {

    private final ReservedSendContext reservedContext;
    private final SseEmitter emitter;
    private final AtomicBoolean clientClosed = new AtomicBoolean(false);
    private final AtomicBoolean workerSubmitted = new AtomicBoolean(false);
    private final AtomicBoolean requestPrepared = new AtomicBoolean(false);
    private volatile String waitingPermitId;
    private volatile String activePermitId;

    public StreamRequestState(ReservedSendContext reservedContext, SseEmitter emitter) {
        this.reservedContext = reservedContext;
        this.emitter = emitter;
    }
}
