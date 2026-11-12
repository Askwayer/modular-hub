package com.vita.auth.interceptor;

import cn.dev33.satoken.stp.StpLogic;
import com.vita.auth.context.LoginUserInfoContextLoader;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * 每个请求实时加载有效账号，用户状态和管理标识不从旧 Session 恢复。
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LoginUserContextInterceptor implements AsyncHandlerInterceptor {

    private final StpLogic stpLogic;
    private final ObjectProvider<LoginUserInfoContextLoader> loaderProvider;

    public LoginUserContextInterceptor(StpLogic stpLogic,
                                       ObjectProvider<LoginUserInfoContextLoader> loaderProvider) {
        this.stpLogic = stpLogic;
        this.loaderProvider = loaderProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LoginUserInfoModelContext.removeLoginUserInfo();
        Long userId = resolveUserId(stpLogic.getLoginIdDefaultNull());
        if (userId == null) {
            return true;
        }
        LoginUserInfoContextLoader loader = loaderProvider.getIfAvailable();
        if (loader == null) {
            return true;
        }
        // 加载失败不降级读取旧资料；缺少有效上下文时由后续登录校验统一拒绝。
        LoginUserInfoModel userInfo = loader.loadByUserId(userId);
        if (userInfo != null && userId.equals(userInfo.getId())) {
            LoginUserInfoModelContext.setLoginUserInfo(userInfo);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                               Object handler) {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    private Long resolveUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            long userId = Long.parseLong(String.valueOf(loginId));
            return userId > 0 ? userId : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
