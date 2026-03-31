package com.vita.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * 通用状态枚举。
 *
 * <p>统一约定：1-启用，0-禁用。</p>
 *
 * @author zhuningkang
 */
@Getter
public enum CommonStatusEnum {

    DISABLED((byte) 0, "禁用"),
    ENABLED((byte) 1, "启用");

    private final Byte code;
    private final String desc;

    CommonStatusEnum(Byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static boolean isEnabled(Byte code) {
        return Objects.equals(ENABLED.code, code);
    }

    public static boolean isDisabled(Byte code) {
        return Objects.equals(DISABLED.code, code);
    }
}
