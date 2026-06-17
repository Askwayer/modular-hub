package com.vita.log.persistence;

import java.time.LocalDateTime;

/**
 * 登录成功后用户最近登录信息更新扩展点，由上层模块提供具体实现。
 *
 * @author znk
 */
public interface LoginUserInfoPersistence {

    /**
     * 更新用户最近登录信息。
     *
     * @param userId 用户ID
     * @param loginTime 登录时间
     * @param loginAddress 登录IP
     */
    void updateLoginInfo(Long userId, LocalDateTime loginTime, String loginAddress);
}
