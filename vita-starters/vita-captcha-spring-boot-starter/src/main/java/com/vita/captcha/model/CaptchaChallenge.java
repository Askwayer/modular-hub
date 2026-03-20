package com.vita.captcha.model;

import com.vita.captcha.enums.CaptchaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.model
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码挑战结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaChallenge implements Serializable {

    private static final long serialVersionUID = -7806775106475537837L;

    private String captchaId;

    private CaptchaType type;

    private LocalDateTime expireAt;

    private Object payload;
}
