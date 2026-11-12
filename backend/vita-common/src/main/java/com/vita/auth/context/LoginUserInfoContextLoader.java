package com.vita.auth.context;

import com.vita.auth.model.LoginUserInfoModel;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.context
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 登录用户上下文加载器
 * @Version: 1.0
 */
public interface LoginUserInfoContextLoader {

    /**
     * 根据用户 ID 实时加载启用且未删除的账号，不使用跨请求缓存。
     *
     * @param userId 用户 ID
     * @return 当前用户资料；账号停用、删除或不存在时返回 null
     */
    LoginUserInfoModel loadByUserId(Long userId);
}
