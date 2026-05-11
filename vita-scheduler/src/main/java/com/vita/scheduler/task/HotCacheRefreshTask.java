package com.vita.scheduler.task;

import com.vita.hot.service.IHotSourceService;
import com.vita.hot.vo.HotSourceStatusVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.scheduler.task
 * @Author: Codex
 * @CreateTime: 2026-05-11
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
    @Scheduled(cron = "${vita.hot.refresh-cron:0 0/10 * * * ?}")
    public void refreshAllEnabledSources() {
        List<HotSourceStatusVo> statusList = hotSourceService.refreshAllEnabledSources();
        LOGGER.info("热榜缓存刷新完成，sourceCount={}", statusList.size());
    }
}
