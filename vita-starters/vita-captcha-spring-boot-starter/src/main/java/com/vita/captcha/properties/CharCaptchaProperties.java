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
 * @Description: 图形文本验证码配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "vita.captcha.char")
public class CharCaptchaProperties {

    private int width = 160;

    private int height = 50;

    private int length = 6;

    private boolean caseSensitive = true;

    private String charString = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private String border = "no";

    private String fontNames = "Arial,Courier";

    private int fontSize = 38;

    private String fontColor = "43,57,85";

    private String noiseColor = "120,130,160";

    private String backgroundFrom = "247,248,252";

    private String backgroundTo = "223,228,238";
}
