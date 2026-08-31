package com.vita.app.user.service;

import com.vita.app.user.entity.AppUser;
import com.vita.app.user.model.AppUserRegisterCommand;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.service
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户认证领域服务
 * @Version: 1.0
 */
public interface IAppUserService {

    /**
     * 注册App用户，注册完成后不创建登录态。
     *
     * @param command 注册命令
     * @return 已创建用户
     */
    AppUser register(AppUserRegisterCommand command);

    /**
     * 校验用户名密码并返回启用用户。
     *
     * @param userName 用户名
     * @param password 原始密码
     * @return 认证通过的用户
     */
    AppUser authenticate(String userName, String password);

    /**
     * 查询并校验用户处于启用状态。
     *
     * @param userId 用户ID
     * @return 启用用户
     */
    AppUser getRequiredEnabledById(Long userId);

    /**
     * 记录最近一次成功登录信息。
     *
     * @param userId       用户ID
     * @param loginAddress 登录地址
     */
    void recordLogin(Long userId, String loginAddress);
}
