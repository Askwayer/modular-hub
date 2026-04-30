package com.vita.aichat.chat.support;

import com.vita.interceptor.RequestTraceInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * WebFlux HTTP 支撑工具。
 */
public final class AiChatReactiveHttpSupport {

    public static final String NDJSON_CONTENT_TYPE_VALUE = "application/x-ndjson";

    public static final MediaType NDJSON_MEDIA_TYPE = MediaType.parseMediaType(NDJSON_CONTENT_TYPE_VALUE);

    private AiChatReactiveHttpSupport() {
    }

    public static boolean isStreamRequest(ServerWebExchange exchange) {
        if (exchange == null) {
            return false;
        }
        String path = exchange.getRequest().getPath().value();
        if (path != null && path.endsWith("/messages/stream")) {
            return true;
        }
        return exchange.getRequest().getHeaders().getAccept().stream()
                .anyMatch(type -> NDJSON_MEDIA_TYPE.isCompatibleWith(type));
    }

    public static String resolveRequestId(ServerWebExchange exchange) {
        if (exchange == null) {
            return "";
        }
        Object requestId = exchange.getAttribute(RequestTraceInterceptor.REQUEST_ID_KEY);
        if (requestId != null) {
            return String.valueOf(requestId);
        }
        return resolveRequestId(exchange.getRequest());
    }

    public static String resolveRequestId(ServerHttpRequest request) {
        if (request == null) {
            return "";
        }
        String requestId = request.getHeaders().getFirst(RequestTraceInterceptor.REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = request.getHeaders().getFirst(RequestTraceInterceptor.REQUEST_ID_KEY);
        }
        return requestId == null ? "" : requestId;
    }

    public static boolean containsNdjsonAccept(ServerHttpRequest request) {
        if (request == null) {
            return false;
        }
        return request.getHeaders().getAccept().stream()
                .anyMatch(type -> NDJSON_MEDIA_TYPE.isCompatibleWith(type));
    }

    public static boolean isAuthorizationHeader(String headerName) {
        return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName);
    }
}
