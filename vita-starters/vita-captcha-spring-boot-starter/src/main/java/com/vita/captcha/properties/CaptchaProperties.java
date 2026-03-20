package com.vita.captcha.properties;

import com.vita.captcha.enums.CaptchaType;
import com.vita.constant.Constants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.properties
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码基础配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "vita.captcha")
public class CaptchaProperties {

    private boolean enabled;

    private CaptchaType captchaType = CaptchaType.SLIDE;

    private StoreType store = StoreType.AUTO;

    private String keyPrefix = "auth:captcha:";

    private Duration ttl = Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION);

    private int maxFailures = 5;

    public enum StoreType {
        AUTO,
        REDIS,
        IN_MEMORY
    }
}
