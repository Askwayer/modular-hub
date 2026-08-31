package com.vita.app.user.support;

import com.vita.crypto.Sm4Utils;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.support
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App密码SM4编码适配器
 * @Version: 1.0
 */
@Component
public class AppPasswordCodec {

    /**
     * 复用管理端SM4密码编码规则，保证同一套密码密文语义。
     *
     * @param rawPassword 原始密码
     * @return SM4 Base64密文
     */
    public String encode(String rawPassword) {
        return Sm4Utils.encryptToBase64(rawPassword);
    }

    /**
     * 按管理端相同规则重新加密并比较密文。
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 数据库存储密文
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return Objects.equals(encode(rawPassword), encodedPassword);
    }
}
