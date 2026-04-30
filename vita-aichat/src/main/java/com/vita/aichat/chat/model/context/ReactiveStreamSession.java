package com.vita.aichat.chat.model.context;

import lombok.Data;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebFlux 流式会话状态。
 */
@Data
public class ReactiveStreamSession {

    private final ReservedSendContext reservedContext;

    private final AtomicBoolean cleanupStarted = new AtomicBoolean(false);

    private volatile String waitingPermitId;

    private volatile String activePermitId;

    private volatile boolean waited;

    private volatile boolean conversationLockAcquired;

    private volatile PreparedSendContext preparedContext;
}
