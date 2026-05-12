package com.vita.hot.property;

import com.vita.hot.constant.HotConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.property
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜业务配置
 * @Version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "vita.hot")
public class HotProperty {

    private List<String> enabledSources = new ArrayList<>(HotConstants.DEFAULT_ENABLED_SOURCES);

    private String refreshCron = HotConstants.DEFAULT_REFRESH_CRON;

    private Long staleThresholdMinutes = HotConstants.DEFAULT_STALE_THRESHOLD_MINUTES;

    private Python python = new Python();

    @Data
    public static class Python {
        private String baseUrl = "http://127.0.0.1:8000";
        private String apiKey;
        private Integer timeoutMs = 8000;
    }
}
