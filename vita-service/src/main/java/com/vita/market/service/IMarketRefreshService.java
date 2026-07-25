package com.vita.market.service;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.service
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 行情采集调度入口
 * @Version: 1.0
 */
public interface IMarketRefreshService {

    boolean triggerManualRefresh();

    void refreshScheduled();

    int syncStockDictionary();

    void refreshTradeCalendar();
}
