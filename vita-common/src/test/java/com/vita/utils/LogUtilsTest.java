package com.vita.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilsTest {

    @Test
    void toRequestLogShouldMaskSensitiveFieldsAndSkipMultipartFiles() {
        RequestPayload payload = new RequestPayload();
        payload.setUsername("admin");
        payload.setPassword("secret-password");
        payload.setToken("secret-token");

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        String requestLog = LogUtils.toRequestLog(new Object[]{payload, file}, null);

        assertThat(requestLog).contains("\"username\":\"admin\"");
        assertThat(requestLog).contains("\"password\":\"***\"");
        assertThat(requestLog).contains("\"token\":\"***\"");
        assertThat(requestLog).doesNotContain("secret-password");
        assertThat(requestLog).doesNotContain("secret-token");
        assertThat(requestLog).doesNotContain("avatar.png");
    }

    @Test
    void isFilterObjectShouldRecognizeServletArtifacts() {
        assertThat(LogUtils.isFilterObject(new MockHttpServletRequest())).isTrue();
        assertThat(LogUtils.isFilterObject(new MockHttpServletResponse())).isTrue();
    }

    @Test
    void toResponseLogShouldTruncateOversizedContent() {
        String content = "a".repeat(2500);

        String responseLog = LogUtils.toResponseLog(Map.of("content", content));

        assertThat(responseLog).hasSizeLessThanOrEqualTo(2000);
        assertThat(responseLog).endsWith("...");
    }

    @Test
    void toErrorLogShouldKeepExceptionSummary() {
        String errorLog = LogUtils.toErrorLog(new IllegalStateException("boom"));

        assertThat(errorLog).isEqualTo("IllegalStateException: boom");
    }

    private static final class RequestPayload {

        private String username;

        private String password;

        private String token;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
