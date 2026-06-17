package com.vita.hot.property;

import com.vita.hot.constant.HotConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

    /**
     * 数据过时阈值（分钟）
     */
    private Long staleThresholdMinutes = HotConstants.DEFAULT_STALE_THRESHOLD_MINUTES;

    /**
     * 热榜源注册信息缓存过期时间（分钟）
     */
    private Long registryCacheTtlMinutes = HotConstants.HOT_SOURCE_META_TTL_MINUTES;
}
