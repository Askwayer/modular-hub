package com.vita.aichat.chat.filter;

import cn.hutool.core.util.IdUtil;
import com.vita.aichat.chat.model.context.AiChatReactiveRequestContext;
import com.vita.aichat.chat.support.AiChatReactiveBridge;
import com.vita.aichat.chat.support.AiChatReactiveHttpSupport;
import com.vita.aichat.chat.support.AiChatTokenAuthenticator;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.interceptor.RequestTraceInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Aichat WebFlux 请求上下文过滤器。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class AiChatReactiveContextFilter implements WebFilter {

    private final AiChatTokenAuthenticator aiChatTokenAuthenticator;

    public AiChatReactiveContextFilter(AiChatTokenAuthenticator aiChatTokenAuthenticator) {
        this.aiChatTokenAuthenticator = aiChatTokenAuthenticator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = AiChatReactiveHttpSupport.resolveRequestId(exchange.getRequest());
        if (!StringUtils.hasText(requestId)) {
            requestId = IdUtil.fastSimpleUUID();
        }
        exchange.getAttributes().put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
        exchange.getResponse().getHeaders().set(RequestTraceInterceptor.REQUEST_ID_HEADER, requestId);

        String path = exchange.getRequest().getPath().value();
        if (path == null || !path.contains("/chat")) {
            AiChatReactiveRequestContext requestContext = AiChatReactiveRequestContext.builder()
                    .requestId(requestId)
                    .build();
            return chain.filter(exchange)
                    .contextWrite(context -> context.put(AiChatReactiveBridge.REQUEST_CONTEXT_KEY, requestContext));
        }

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            AiChatReactiveRequestContext requestContext = AiChatReactiveRequestContext.builder()
                    .requestId(requestId)
                    .build();
            return chain.filter(exchange)
                    .contextWrite(context -> context.put(AiChatReactiveBridge.REQUEST_CONTEXT_KEY, requestContext));
        }

        var loginUserInfo = aiChatTokenAuthenticator.authenticate(exchange.getRequest());
        if (loginUserInfo == null) {
            return Mono.error(new ServiceException(GlobalErrorCode.UNAUTHORIZED));
        }

        AiChatReactiveRequestContext requestContext = AiChatReactiveRequestContext.builder()
                .requestId(requestId)
                .loginUserInfo(loginUserInfo)
                .build();
        exchange.getAttributes().put(AiChatReactiveBridge.REQUEST_CONTEXT_KEY, requestContext);
        return chain.filter(exchange)
                .contextWrite(context -> context.put(AiChatReactiveBridge.REQUEST_CONTEXT_KEY, requestContext));
    }
}
