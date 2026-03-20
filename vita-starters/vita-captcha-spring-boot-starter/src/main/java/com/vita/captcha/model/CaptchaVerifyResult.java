package com.vita.captcha.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.model
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码校验结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVerifyResult implements Serializable {

    private static final long serialVersionUID = 7571248626258286205L;

    private boolean success;

    private String message;

    private Integer remainingAttempts;

    /**
     * 构建校验成功结果。
     *
     * @param message 结果消息
     * @return 校验成功结果
     */
    public static CaptchaVerifyResult success(String message) {
        return new CaptchaVerifyResult(true, message, null);
    }

    /**
     * 构建校验失败结果。
     *
     * @param message 结果消息
     * @param remainingAttempts 剩余重试次数
     * @return 校验失败结果
     */
    public static CaptchaVerifyResult failure(String message, Integer remainingAttempts) {
        return new CaptchaVerifyResult(false, message, remainingAttempts);
    }
}
