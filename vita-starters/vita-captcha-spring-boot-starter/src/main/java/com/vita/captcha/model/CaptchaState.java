package com.vita.captcha.model;

import com.vita.captcha.enums.CaptchaType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.model
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码服务端状态
 */
@Data
public class CaptchaState implements Serializable {

    private static final long serialVersionUID = 4342723867918010296L;

    private String captchaId;

    private CaptchaType type;

    private LocalDateTime expireAt;

    private int failureCount;

    private int maxFailureCount;

    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 判断当前验证码状态是否已过期。
     *
     * @return 是否已过期
     */
    public boolean isExpired() {
        return expireAt != null && expireAt.isBefore(LocalDateTime.now());
    }

    /**
     * 计算当前验证码剩余重试次数。
     *
     * @return 剩余重试次数
     */
    public int remainingAttempts() {
        return Math.max(0, maxFailureCount - failureCount);
    }
}
