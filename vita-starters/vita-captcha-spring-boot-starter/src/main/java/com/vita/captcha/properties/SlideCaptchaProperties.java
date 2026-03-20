package com.vita.captcha.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.properties
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 滑动验证码配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "vita.captcha.slide")
public class SlideCaptchaProperties {

    private int width = 320;

    private int height = 80;

    private int tolerance = 6;

    private int minTrackPoints = 3;

    private int handleWidth = 44;

    private String trackColor = "224,229,238";

    private String goalColor = "94,129,255";
}
