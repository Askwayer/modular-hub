package com.vita.scheduler.task;

import com.vita.hot.service.HotRefreshExecutionResult;
import com.vita.hot.service.IHotSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.scheduler.task
 * @Author: Codex
 * @CreateTime: 2026-05-12
 * @Description: 热榜缓存定时刷新任务
 * @Version: 1.0
 */
@Component
public class HotCacheRefreshTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotCacheRefreshTask.class);

    private final IHotSourceService hotSourceService;

    public HotCacheRefreshTask(IHotSourceService hotSourceService) {
        this.hotSourceService = hotSourceService;
    }

    /**
     * 定时刷新全部启用热榜缓存。
     */
    @Scheduled(cron = "${vita.hot.refresh-cron:0 0/15 * * * ?}")
    public void refreshAllEnabledSources() {
        LOGGER.info("热榜缓存定时刷新开始，step=fetch-source-registry");
        HotRefreshExecutionResult result = hotSourceService.refreshAllEnabledSourcesForScheduler();
        if (result.skipped()) {
            LOGGER.warn("热榜缓存定时刷新跳过，reason=source-registry-unavailable-without-cache");
            return;
        }
        if (result.registryFallbackToCache()) {
            LOGGER.warn("热榜缓存定时刷新完成，sourceCount={}, registrySource=cache", result.statusList().size());
            return;
        }
        LOGGER.info("热榜缓存定时刷新完成，sourceCount={}, registrySource=remote", result.statusList().size());
    }
}
