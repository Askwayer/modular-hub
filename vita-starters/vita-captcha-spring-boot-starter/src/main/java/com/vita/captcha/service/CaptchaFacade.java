package com.vita.captcha.service;

import com.vita.captcha.dto.CaptchaVerifyRequest;
import com.vita.captcha.model.CaptchaChallenge;
import com.vita.captcha.model.CaptchaVerifyResult;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.service
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 统一验证码门面
 */
public interface CaptchaFacade {

    /**
     * 生成当前启用的验证码挑战。
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
