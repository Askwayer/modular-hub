package com.vita.log.interceptor;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.auth.context.LoginUserInfoModelContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;


/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.interceptor
 * @Author: znk
 * @CreateTime: 2026-03-12  00:00:00
 * @Description: 请求级上下文拦截器
 * @Version: 1.0
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RequestTraceInterceptor implements AsyncHandlerInterceptor {

    public static final String REQUEST_ID_KEY = "requestId";

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * 预处理当前请求上下文。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器对象
     * @return true 表示继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        clearRequestContext();

        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID_KEY, requestId);
        return true;
    }

    /**
     * 清理当前请求上下文。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器对象
     * @param ex       异常信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        clearRequestContext();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                               Object handler) {
        clearRequestContext();
    }

    /**
     * 解析当前请求标识。
     *
     * @param request 请求对象
     * @return 请求 ID
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (CharSequenceUtil.isBlank(requestId)) {
            requestId = request.getHeader(REQUEST_ID_KEY);
        }
        return CharSequenceUtil.isNotBlank(requestId) ? requestId : IdUtil.fastSimpleUUID();
    }

    /**
     * 清理线程中的请求上下文信息。
     */
    private void clearRequestContext() {
        LoginUserInfoModelContext.removeLoginUserInfo();
        MDC.remove(REQUEST_ID_KEY);
    }
}
