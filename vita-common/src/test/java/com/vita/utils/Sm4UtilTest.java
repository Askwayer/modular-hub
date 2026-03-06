package com.vita.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Sm4UtilTest {

    private static final String VALID_KEY = "1234567890abcdef";

    @Test
    void shouldEncryptAndDecrypt() {
        String plainText = "hello-sm4-中文";

        String cipherText = Sm4Util.encryptToBase64(plainText, VALID_KEY);
        String decryptedText = Sm4Util.decryptFromBase64(cipherText, VALID_KEY);

        assertNotEquals(plainText, cipherText);
        assertEquals(plainText, decryptedText);
    }

    @Test
    void shouldRejectInvalidKeyLength() {
        IllegalArgumentException encryptException =
                assertThrows(IllegalArgumentException.class, () -> Sm4Util.encryptToBase64("hello", "short-key"));
        assertEquals("key must be exactly 16 bytes in UTF-8", encryptException.getMessage());

        IllegalArgumentException decryptException =
                assertThrows(IllegalArgumentException.class, () -> Sm4Util.decryptFromBase64("YWJjZA==", "123"));
        assertEquals("key must be exactly 16 bytes in UTF-8", decryptException.getMessage());
    }

    @Test
    void shouldRejectEmptyEncryptInput() {
        IllegalArgumentException plainTextException =
                assertThrows(IllegalArgumentException.class, () -> Sm4Util.encryptToBase64("", VALID_KEY));
        assertEquals("plainText must not be empty", plainTextException.getMessage());

        IllegalArgumentException keyException =
                assertThrows(IllegalArgumentException.class, () -> Sm4Util.encryptToBase64("hello", ""));
        assertEquals("key must not be empty", keyException.getMessage());
    }

    @Test
    void shouldRejectEmptyDecryptInput() {
        IllegalArgumentException cipherException =
                assertThrows(IllegalArgumentException.class, () -> Sm4Util.decryptFromBase64("", VALID_KEY));
        assertEquals("cipherText must not be empty", cipherException.getMessage());

        IllegalArgumentException keyException =
                assertThrows(IllegalArgumentException.class, () -> Sm4Util.decryptFromBase64("YWJjZA==", ""));
        assertEquals("key must not be empty", keyException.getMessage());
    }
}
