package com.vita.interceptor;

import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.constant.AuthConstants;
import com.vita.core.context.LoginUserInfoContextLoader;
import com.vita.core.context.LoginUserInfoModelContext;
import com.vita.core.model.LoginUserInfoModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
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

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ObjectProvider<LoginUserInfoContextLoader> loginUserInfoContextLoaderProvider;

    public RequestTraceInterceptor(ObjectProvider<LoginUserInfoContextLoader> loginUserInfoContextLoaderProvider) {
        this.loginUserInfoContextLoaderProvider = loginUserInfoContextLoaderProvider;
    }

    /**
     * 请求前处理器，在请求进入控制器方法之前调用，
     * 负责准备请求级上下文，包括解析请求 ID 和登录用户信息，并将其存储在 MDC 和 LoginUserInfoModelContext 中，
     * 以便后续业务逻辑使用。
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  chosen handler to execute, for type and/or instance evaluation
     * @return true / false
     */
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

        LoginUserInfoModel loginUserInfoModel = resolveStoredLoginUserInfo(StpUtil.getSession().get(AuthConstants.LOGIN_USER_INFO));
        if (loginUserInfoModel != null) {
            LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);
            return;
        }

        restoreLoginUserContext();
    }

    /**
     * 解析 Session 中缓存的登录用户快照。
     *
     * @param sessionValue Session 缓存值
     * @return 登录用户快照
     */
    private LoginUserInfoModel resolveStoredLoginUserInfo(Object sessionValue) {
        if (sessionValue instanceof LoginUserInfoModel model) {
            return model;
        }
        return sessionValue != null ? BeanUtil.toBean(sessionValue, LoginUserInfoModel.class) : null;
    }

    /**
     * Session 未命中登录用户快照时，根据 loginId 重建并回写 Session 与线程上下文。
     */
    private void restoreLoginUserContext() {
        Long userId = resolveUserId(StpUtil.getLoginIdDefaultNull());
        if (userId == null) {
            return;
        }
        LoginUserInfoContextLoader loader = loginUserInfoContextLoaderProvider.getIfAvailable();
        if (loader == null) {
            return;
        }
        LoginUserInfoModel loginUserInfoModel = loader.loadByUserId(userId);
        if (loginUserInfoModel == null) {
            return;
        }
        StpUtil.getSession().set(AuthConstants.LOGIN_USER_INFO, loginUserInfoModel);
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);
    }

    /**
     * 将任意登录 ID 安全转换为 Long。
     *
     * @param loginId 登录 ID
     * @return 用户 ID；无法转换时返回 null
     */
    private Long resolveUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException ex) {
            return null;
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
