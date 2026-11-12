package com.vita.auth.service;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: 认证令牌上下文服务
 * @Version: 1.0
 */
public interface IAuthTokenService {

    /**
     * 建立当前用户的登录态。
     *
     * @param userId 用户ID
     */
    void login(Long userId);

    /**
     * 校验当前请求是否已经登录。
     */
    void checkLogin();

    /**
     * 注销当前登录态。
     */
    void logout();

    /**
     * 获取当前 token 值。
     *
     * @return token 值
     */
    String getTokenValue();

    /**
     * 获取 token 剩余有效期。
     *
     * @return 剩余有效期，单位秒
     */
    Long getTokenTimeout();

    /**
     * 获取 token 名称。
     *
     * @return token 名称
     */
    String getTokenName();

    /**
     * 获取 token 前缀。
     *
     * @return token 前缀
     */
    String getTokenPrefix();

    /**
     * 获取当前登录用户ID，未登录时返回空。
     *
     * @return 登录用户ID
     */
    Object getLoginId();
}
