package com.vita.captcha.service;

import com.vita.captcha.model.CaptchaState;

import java.util.Optional;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码状态存储
 */
public interface CaptchaStore {

    /**
     * 保存验证码状态。
     *
     * @param state 验证码状态
     */
    void save(CaptchaState state);

    /**
     * 加载验证码状态。
     *
     * @param captchaId 验证码标识
     * @return 验证码状态
     */
    Optional<CaptchaState> load(String captchaId);

    /**
     * 消费验证码状态。
     *
     * @param captchaId 验证码标识
     * @return 被消费的验证码状态
     */
    Optional<CaptchaState> consume(String captchaId);

    /**
     * 累加验证码失败次数。
     *
     * @param captchaId 验证码标识
     * @return 更新后的验证码状态
     */
    Optional<CaptchaState> incrementFailCount(String captchaId);

    /**
     * 删除验证码状态。
     *
     * @param captchaId 验证码标识
     */
    void delete(String captchaId);
}
