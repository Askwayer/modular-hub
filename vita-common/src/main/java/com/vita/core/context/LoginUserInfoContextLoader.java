package com.vita.core.context;

import com.vita.core.model.LoginUserInfoModel;

/**
 * 登录用户上下文加载器。
 *
 * <p>用于在 Sa-Token Session 未命中登录用户快照时，
 * 根据当前登录 ID 重建登录用户信息并回填上下文。</p>
 */
public interface LoginUserInfoContextLoader {

    /**
     * 根据用户 ID 重建登录用户快照。
     *
     * @param userId 用户 ID
     * @return 登录用户快照；无法重建时返回 null
     */
    LoginUserInfoModel loadByUserId(Long userId);
}
