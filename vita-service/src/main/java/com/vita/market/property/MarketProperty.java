package com.vita.market.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.property
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: A股行情采集配置
 * @Version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "vita.market")
public class MarketProperty {

    private String aktoolsBaseUrl = "http://127.0.0.1:18001";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 8000;
    private int retryTimes = 1;
    private int snapshotTtlDays = 35;
    private int fundPointRetentionDays = 30;
    private int manualRefreshIntervalSeconds = 30;
    private int refreshLockLeaseSeconds = 35;
}
