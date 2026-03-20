package com.vita.captcha.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.enums
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码类型枚举
 */
public enum CaptchaType {

    /**
     * 图形文本验证码。
     */
    CHAR("char"),

    /**
     * 图形答案验证码。
     */
    MATH("math"),

    /**
     * 滑动验证码。
     */
    SLIDE("slide");

    private final String value;

    CaptchaType(String value) {
        this.value = value;
    }

    /**
     * 将字符串转换为验证码类型。
     *
     * @param value 验证码类型字符串
     * @return 验证码类型
     */
    @JsonCreator
    public static CaptchaType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "char" -> CHAR;
            case "math", "arithmetic" -> MATH;
            case "slide", "slider" -> SLIDE;
            default -> throw new IllegalArgumentException("没有支持的验证码类型: " + value);
        };
    }

    /**
     * 获取序列化后的验证码类型值。
     *
     * @return 小写验证码类型值
     */
    @JsonValue
    public String getValue() {
        return value.toLowerCase(Locale.ROOT);
    }
}
