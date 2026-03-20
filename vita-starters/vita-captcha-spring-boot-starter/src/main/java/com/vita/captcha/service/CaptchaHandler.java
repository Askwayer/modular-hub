package com.vita.captcha.service;

import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.enums.CaptchaType;
import com.vita.captcha.model.CaptchaChallenge;
import com.vita.captcha.model.CaptchaVerifyResult;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 单个验证码模式处理器
 */
public interface CaptchaHandler {

    /**
     * 获取当前处理器支持的验证码类型。
     *
     * @return 验证码类型
     */
    CaptchaType getType();

    /**
     * 生成验证码挑战。
     *
     * @return 验证码挑战结果
     */
    CaptchaChallenge generate();

    /**
     * 校验验证码请求。
     *
     * @param request 验证码校验请求
     * @return 验证码校验结果
     */
    CaptchaVerifyResult verify(CaptchaVerifyRequest request);
}
