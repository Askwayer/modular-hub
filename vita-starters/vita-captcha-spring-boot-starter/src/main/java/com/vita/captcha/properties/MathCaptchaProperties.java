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
 * @Description: 图形答案验证码配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "vita.captcha.math")
public class MathCaptchaProperties {

    private int width = 180;

    private int height = 50;

    private int minNumber = 1;

    private int maxNumber = 30;

    private String operators = "+,-";

    private String border = "no";

    private String fontNames = "Arial,Courier";

    private int fontSize = 38;

    private String fontColor = "43,57,85";

    private String noiseColor = "120,130,160";

    private String backgroundFrom = "247,248,252";

    private String backgroundTo = "223,228,238";
}
