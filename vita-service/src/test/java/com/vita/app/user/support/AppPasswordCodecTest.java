package com.vita.app.user.support;

import com.vita.crypto.Sm4Utils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.support
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App密码SM4编码测试
 * @Version: 1.0
 */
class AppPasswordCodecTest {

    @Test
    void shouldReuseAdminSm4Encoding() {
        AppPasswordCodec codec = new AppPasswordCodec();
        String encoded = codec.encode("app-password-123");

        assertThat(encoded).isEqualTo(Sm4Utils.encryptToBase64("app-password-123"));
        assertThat(codec.matches("app-password-123", encoded)).isTrue();
        assertThat(codec.matches("wrong-password", encoded)).isFalse();
    }
}
