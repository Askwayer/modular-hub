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

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginUserInfoModelContext.class);

    private static final ThreadLocal<LoginUserInfoModel> loginUserInfoThreadLocal = new ThreadLocal<>();

    /**
     * 获取当前登录用户
     */
    public static LoginUserInfoModel getLoginUserInfo() {
        return loginUserInfoThreadLocal.get();
    }

    /**
     * 设置当前登录用户信息
     *
     * @param model 登录用户信息模型
     */
    public static void setLoginUserInfo(LoginUserInfoModel model) {
        loginUserInfoThreadLocal.set(model);
    }

    /**
     * 清除当前登录用户信息
     */
    public static void removeLoginUserInfo() {
        loginUserInfoThreadLocal.remove();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 登录用户ID
     */
    public static Long getLoginUserId() {

        LoginUserInfoModel model = loginUserInfoThreadLocal.get();
        if (model != null) {
            return model.getId();
        } else {
            LOGGER.warn("当前线程没有登录用户信息");
            return null;
        }
    }
}
