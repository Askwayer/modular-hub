package com.vita.hot.property;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
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
public class HotDailyApiProperty {

    @Value("${vita.hot.daily-hot-api.base-url:http://127.0.0.1:18000}")
    private String baseUrl;

    @Value("${vita.hot.daily-hot-api.api-key:}")
    private String apiKey;

    @Value("${vita.hot.daily-hot-api.timeout-ms:8000}")
    private Integer timeoutMs;
}
