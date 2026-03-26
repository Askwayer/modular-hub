package com.vita.interceptor;

import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.constant.AuthConstants;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.model.LoginUserInfoModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.log.model
 * @Author znk
 * @CreateTime 2026-03-12
 * @Description: 请求级上下文拦截器，负责 requestId 与登录用户上下文的准备和清理。
 */
@Component
public class RequestTraceInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_KEY = "requestId";

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        clearRequestContext();

        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID_KEY, requestId);
        populateLoginUserContext();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        clearRequestContext();
    }

    /**
     * 请求ID解析器，优先从请求头中获取，如果不存在则生成一个新的 UUID 作为请求 ID，并将其放入 MDC 中以便日志记录使用。
     *
     * @param request 请求头
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
     * <p>
     * 登录用户上下文准备器，从 Sa-Token 会话中获取登录用户信息，
     * 并将其存储在 LoginUserInfoModelContext 中，以便后续业务逻辑使用。
     * </p>
     */
    private void populateLoginUserContext() {
        try {
            if (!StpUtil.isLogin()) {
                return;
            }
        } catch (SaTokenContextException ex) {
            return;
        }

        Object sessionValue = StpUtil.getSession().get(AuthConstants.LOGIN_USER_INFO);
        if (sessionValue instanceof LoginUserInfoModel model) {
            LoginUserInfoModelContext.setLoginUserInfo(model);
            return;
        }
        if (sessionValue != null) {
            LoginUserInfoModelContext.setLoginUserInfo(BeanUtil.toBean(sessionValue, LoginUserInfoModel.class));
        }
    }

    /**
     * <p>
     * 请求上下文清理器，在请求处理完成后调用，负责清理 LoginUserInfoModelContext 中的登录用户信息
     * 以及从 MDC 中移除请求 ID，以防止内存泄漏和上下文污染。
     * </p>
     */
    private void clearRequestContext() {
        LoginUserInfoModelContext.removeLoginUserInfo();
        MDC.remove(REQUEST_ID_KEY);
    }
}
