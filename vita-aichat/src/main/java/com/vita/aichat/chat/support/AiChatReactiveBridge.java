package com.vita.aichat.chat.support;

import com.vita.aichat.chat.model.context.AiChatReactiveRequestContext;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.interceptor.RequestTraceInterceptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * WebFlux 阻塞桥。
 *
 * <p>负责把 Reactor Context 中的 requestId / 登录用户桥接到 ThreadLocal 与 MDC，避免阻塞段直接跑在 event loop。</p>
 */
@Component
public class AiChatReactiveBridge {

    public static final String REQUEST_CONTEXT_KEY = AiChatReactiveRequestContext.class.getName();

    private final Scheduler blockingScheduler;

    public AiChatReactiveBridge(@Qualifier("aiChatTaskExecutor") Executor aiChatExecutor) {
        this.blockingScheduler = Schedulers.fromExecutor(aiChatExecutor);
    }

    public Scheduler blockingScheduler() {
        return blockingScheduler;
    }

    public AiChatReactiveRequestContext currentRequestContext(ContextView contextView) {
        if (contextView == null || !contextView.hasKey(REQUEST_CONTEXT_KEY)) {
            return AiChatReactiveRequestContext.builder().build();
        }
        return contextView.get(REQUEST_CONTEXT_KEY);
    }

    public <T> Mono<T> blocking(AiChatReactiveRequestContext requestContext, Callable<T> callable) {
        return Mono.fromCallable(() -> executeWithContext(requestContext, callable))
                .subscribeOn(blockingScheduler);
    }

    public Mono<Void> blockingRunnable(AiChatReactiveRequestContext requestContext, CheckedRunnable runnable) {
        return blocking(requestContext, () -> {
            runnable.run();
            return null;
        }).then();
    }

    public void fireAndForget(AiChatReactiveRequestContext requestContext, CheckedRunnable runnable) {
        blockingRunnable(requestContext, runnable).subscribe();
    }

    private <T> T executeWithContext(AiChatReactiveRequestContext requestContext, Callable<T> callable) throws Exception {
        clearRequestContext();
        if (requestContext != null) {
            if (requestContext.getLoginUserInfo() != null) {
                LoginUserInfoModelContext.setLoginUserInfo(requestContext.getLoginUserInfo());
            }
            if (requestContext.getRequestId() != null && !requestContext.getRequestId().isBlank()) {
                MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestContext.getRequestId());
            }
        }
        try {
            return callable.call();
        } finally {
            clearRequestContext();
        }
    }

    private void clearRequestContext() {
        LoginUserInfoModelContext.removeLoginUserInfo();
        MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
    }

    @FunctionalInterface
    public interface CheckedRunnable {

        void run() throws Exception;
    }
}
