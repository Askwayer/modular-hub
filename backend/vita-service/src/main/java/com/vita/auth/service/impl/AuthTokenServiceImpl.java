package com.vita.auth.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.vita.auth.service.IAuthTokenService;
import org.springframework.stereotype.Service;


/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service.impl
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: Sa-Token 认证令牌上下文服务实现
 * @Version: 1.0
 */
@Service
public class AuthTokenServiceImpl implements IAuthTokenService {

    /**
     * 基于 Sa-Token 建立当前用户登录态。
     *
     * @param userId 用户ID
     */
    @Override
    public void login(Long userId) {
        StpUtil.login(userId);
    }

    /**
     * 校验当前请求是否已经登录。
     */
    @Override
    public void checkLogin() {
        StpUtil.checkLogin();
    }

    /**
     * 清理当前登录态并执行登出。
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前请求对应的 token 值。
     *
     * @return token 值
     */
    @Override
    public String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 获取当前 token 的剩余有效期。
     *
     * @return 剩余有效期，单位秒
     */
    @Override
    public Long getTokenTimeout() {
        return StpUtil.getTokenInfo().getTokenTimeout();
    }

    /**
     * 获取 Sa-Token 配置的令牌名称。
     *
     * @return 令牌名称
     */
    @Override
    public String getTokenName() {
        return SaManager.getConfig().getTokenName();
    }

    /**
     * 获取 Sa-Token 配置的令牌前缀。
     *
     * @return 令牌前缀
     */
    @Override
    public String getTokenPrefix() {
        return SaManager.getConfig().getTokenPrefix();
    }

    /**
     * 获取当前登录用户ID，未登录时返回空。
     *
     * @return 登录用户ID
     */
    @Override
    public Object getLoginId() {
        return StpUtil.getLoginIdDefaultNull();
    }
}
