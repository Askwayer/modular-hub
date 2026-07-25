package com.vita.scheduler.market;

import com.vita.market.service.IMarketRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.scheduler.market
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: A股行情采集定时任务
 * @Version: 1.0
 */
@Component
public class MarketCollectionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketCollectionScheduler.class);

    private final IMarketRefreshService marketRefreshService;

    public MarketCollectionScheduler(IMarketRefreshService marketRefreshService) {
        this.marketRefreshService = marketRefreshService;
    }

    /**
     * 固定延迟40秒触发，具体是否采集由交易日历和交易时段判断。
     */
    @Scheduled(fixedDelay = 40_000L, initialDelay = 10_000L)
    public void refreshMarket() {
        try {
            marketRefreshService.refreshScheduled();
        } catch (RuntimeException e) {
            LOGGER.error("行情定时刷新失败，error={}", e.getMessage(), e);
        }
    }

    /**
     * 每天08:00更新交易日历。
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Shanghai")
    public void refreshTradeCalendar() {
        try {
            marketRefreshService.refreshTradeCalendar();
        } catch (RuntimeException e) {
            LOGGER.error("交易日历定时刷新失败，error={}", e.getMessage(), e);
        }
    }

    /**
     * 每天08:15同步股票字典。
     */
    @Scheduled(cron = "0 15 8 * * *", zone = "Asia/Shanghai")
    public void syncStockDictionary() {
        try {
            int count = marketRefreshService.syncStockDictionary();
            LOGGER.info("股票字典同步完成，count={}", count);
        } catch (RuntimeException e) {
            LOGGER.error("股票字典定时同步失败，error={}", e.getMessage(), e);
        }
    }
}
