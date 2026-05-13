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

    /**
     * 启用的热榜数据源列表
     */
    private List<String> enabledSources = new ArrayList<>();

    /**
     * 热榜数据刷新 Cron 表达式
     */
    private String refreshCron = HotConstants.DEFAULT_REFRESH_CRON;

    /**
     * 数据过时阈值（分钟）
     */
    private Long staleThresholdMinutes = HotConstants.DEFAULT_STALE_THRESHOLD_MINUTES;

    /**
     * 刷新任务并行度
     */
    private Integer refreshParallelism = HotConstants.DEFAULT_REFRESH_PARALLELISM;
}
