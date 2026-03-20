package com.vita.captcha.exception;

import com.vita.core.exception.ErrorCode;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.exception
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码模块错误码
 */
public interface CaptchaErrorCode {

    ErrorCode CAPTCHA_DISABLED = new ErrorCode(1100, "验证码能力未启用");
    ErrorCode CAPTCHA_TYPE_UNSUPPORTED = new ErrorCode(1101, "验证码类型未启用或不支持");
    ErrorCode CAPTCHA_CHALLENGE_MISSING = new ErrorCode(1102, "验证码不存在或已失效");
    ErrorCode CAPTCHA_VERIFY_FAILED = new ErrorCode(1103, "验证码校验失败");
    ErrorCode CAPTCHA_REQUEST_INVALID = new ErrorCode(1104, "验证码请求参数不完整");
}
