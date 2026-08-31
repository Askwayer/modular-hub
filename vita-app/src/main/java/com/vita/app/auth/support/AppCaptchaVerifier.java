package com.vita.app.auth.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.service.CaptchaApplicationService;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.support
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App认证验证码校验器
 * @Version: 1.0
 */
@Component
public class AppCaptchaVerifier {

    private final CaptchaApplicationService captchaApplicationService;

    public AppCaptchaVerifier(CaptchaApplicationService captchaApplicationService) {
        this.captchaApplicationService = captchaApplicationService;
    }

    /**
     * 注册和密码登录必须消费验证码，OAuth回调不重复要求验证码。
     *
     * @param code        验证码答案
     * @param uuid        验证码UUID
     * @param encryptData 验证码加密数据
     * @param mix         混淆值
     */
    public void verify(String code, String uuid, String encryptData, String mix) {
        CaptchaVerifyRequestDto request = new CaptchaVerifyRequestDto();
        request.setCaptchaCode(code);
        request.setUuid(uuid);
        request.setEncryptData(encryptData);
        request.setMix(CharSequenceUtil.isBlank(mix) ? uuid : mix);
        captchaApplicationService.checkCaptcha(request);
    }
}
