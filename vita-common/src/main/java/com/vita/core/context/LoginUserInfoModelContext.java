package com.vita.core.context;

import com.vita.core.model.LoginUserInfoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.context
 * @Author: znk
 * @CreateTime: 2026-03-06  21:12:12
 * @Description: 登录用户信息模型上下文，提供全局访问登录用户信息的方式
 * @Version: 1.0
 */
public class LoginUserInfoModelContext {

    private static final Logger LOG = LoggerFactory.getLogger(LoginUserInfoModelContext.class);

    private static final ThreadLocal<LoginUserInfoModel> loginUserInfoModelThreadLocal = new ThreadLocal<>();

    /**
     * 获取当前登陆会员信息
     *
     * @return LoginInfoVo
     */
    public static LoginUserInfoModel get() {
        return loginUserInfoModelThreadLocal.get();
    }

    /**
     * 设置当前登陆会员信息
     *
     * @param loginUserInfoModel 登录用户信息模型
     */
    public static void setLoginInfo(LoginUserInfoModel loginUserInfoModel) {
        loginUserInfoModelThreadLocal.set(loginUserInfoModel);
    }

    /**
     * 清理当前登陆会员信息
     */
    public static void clear() {
        loginUserInfoModelThreadLocal.remove();
    }

    /**
     * 获取当前登陆会员ID
     *
     * @return id
     */
    public static Long getId() {
        try {
            return loginUserInfoModelThreadLocal.get().getId();
        } catch (Exception e) {
            LOG.error("获取会员ID失败" , e);
            throw e;
        }
    }
}
