package com.vita.aichat.chat.support;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.vita.constant.AuthConstants;
import com.vita.core.context.LoginUserInfoContextLoader;
import com.vita.core.model.LoginUserInfoModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于 token 的 WebFlux 登录态解析器。
 */
@Component
public class AiChatTokenAuthenticator {

    private final ObjectProvider<LoginUserInfoContextLoader> loginUserInfoContextLoaderProvider;

    public AiChatTokenAuthenticator(ObjectProvider<LoginUserInfoContextLoader> loginUserInfoContextLoaderProvider) {
        this.loginUserInfoContextLoaderProvider = loginUserInfoContextLoaderProvider;
    }

    public LoginUserInfoModel authenticate(ServerHttpRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }

        Object loginId;
        try {
            StpUtil.getStpLogic().checkActiveTimeout(token);
            loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }
            StpUtil.getStpLogic().updateLastActiveToNow(token);
        } catch (Exception ex) {
            return null;
        }

        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session == null) {
            return null;
        }

        LoginUserInfoModel loginUserInfo = resolveStoredLoginUserInfo(session.get(AuthConstants.LOGIN_USER_INFO));
        if (loginUserInfo != null) {
            return loginUserInfo;
        }

        Long userId = resolveUserId(loginId);
        if (userId == null) {
            return null;
        }
        LoginUserInfoContextLoader loader = loginUserInfoContextLoaderProvider.getIfAvailable();
        if (loader == null) {
            return null;
        }
        loginUserInfo = loader.loadByUserId(userId);
        if (loginUserInfo != null) {
            session.set(AuthConstants.LOGIN_USER_INFO, loginUserInfo);
        }
        return loginUserInfo;
    }

    private LoginUserInfoModel resolveStoredLoginUserInfo(Object sessionValue) {
        if (sessionValue instanceof LoginUserInfoModel model) {
            return model;
        }
        return sessionValue != null ? BeanUtil.toBean(sessionValue, LoginUserInfoModel.class) : null;
    }

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

    private String resolveToken(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }
        String tokenName = SaManager.getConfig().getTokenName();
        String tokenPrefix = SaManager.getConfig().getTokenPrefix();

        String token = request.getHeaders().getFirst(tokenName);
        if (!StringUtils.hasText(token) && !AiChatReactiveHttpSupport.isAuthorizationHeader(tokenName)) {
            token = request.getHeaders().getFirst("Authorization");
        }
        if (!StringUtils.hasText(token)) {
            HttpCookie cookie = request.getCookies().getFirst(tokenName);
            token = cookie != null ? cookie.getValue() : null;
        }
        if (!StringUtils.hasText(token)) {
            token = request.getQueryParams().getFirst(tokenName);
        }
        return stripPrefix(token, tokenPrefix);
    }

    private String stripPrefix(String token, String tokenPrefix) {
        if (!StringUtils.hasText(token)) {
            return token;
        }
        if (!StringUtils.hasText(tokenPrefix)) {
            return token.trim();
        }
        String prefixWithBlank = tokenPrefix + " ";
        if (token.regionMatches(true, 0, prefixWithBlank, 0, prefixWithBlank.length())) {
            return token.substring(prefixWithBlank.length()).trim();
        }
        if (token.regionMatches(true, 0, tokenPrefix, 0, tokenPrefix.length())) {
            return token.substring(tokenPrefix.length()).trim();
        }
        return token.trim();
    }
}
