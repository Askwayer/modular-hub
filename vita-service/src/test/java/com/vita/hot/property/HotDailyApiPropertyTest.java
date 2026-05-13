package com.vita.hot.property;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.property
 * @Author: Codex
 * @CreateTime: 2026-05-12
 * @Description: 热搜抓取服务配置绑定测试
 * @Version: 1.0
 */
class HotDailyApiPropertyTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindDailyHotApiProperties() {
        contextRunner
                .withPropertyValues(
                        "vita.hot.daily-hot-api.base-url=http://127.0.0.1:18000",
                        "vita.hot.daily-hot-api.api-key=test-key",
                        "vita.hot.daily-hot-api.timeout-ms=9000"
                )
                .run(context -> {
                    HotDailyApiProperty property = context.getBean(HotDailyApiProperty.class);
                    assertEquals("http://127.0.0.1:18000", property.getBaseUrl());
                    assertEquals("test-key", property.getApiKey());
                    assertEquals(9000, property.getTimeoutMs());
                });
    }

    @Configuration
    @EnableConfigurationProperties(HotDailyApiProperty.class)
    static class TestConfiguration {
    }
}
