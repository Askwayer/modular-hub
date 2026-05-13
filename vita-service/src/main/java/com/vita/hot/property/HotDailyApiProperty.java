package com.vita.hot.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.property
 * @Author: znk
 * @CreateTime: 2026-05-12  20:18:46
 * @Description: 热榜 Python 服务配置
 * @Version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "vita.hot.daily-hot-api")
public class HotDailyApiProperty {

    private String baseUrl = "http://127.0.0.1:18000";

    private String apiKey;

    private Integer timeoutMs = 8000;
}
