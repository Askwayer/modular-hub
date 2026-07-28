package com.vita.market.service;

import com.vita.market.model.MarketTradeContext;
import com.vita.market.support.AkToolsMarketClient;
import com.vita.market.support.MarketSnapshotStore;
import com.vita.market.support.MarketTradeClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.service
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 交易日历缓存与当前交易状态服务
 * @Version: 1.0
 */
@Service
public class MarketTradeCalendarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketTradeCalendarService.class);
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final AkToolsMarketClient marketClient;
    private final MarketSnapshotStore snapshotStore;
    private final MarketTradeClock tradeClock;
    private final Object calendarRefreshMonitor = new Object();

    public MarketTradeCalendarService(AkToolsMarketClient marketClient,
                                      MarketSnapshotStore snapshotStore,
                                      MarketTradeClock tradeClock) {
        this.marketClient = marketClient;
        this.snapshotStore = snapshotStore;
        this.tradeClock = tradeClock;
    }

    /**
     * 获取当前交易上下文。缓存不存在时尝试拉取，拉取失败则返回 UNKNOWN。
     *
     * @return 当前交易上下文
     */
    public MarketTradeContext current() {
        List<LocalDate> tradeDates = snapshotStore.getTradeCalendar();
        if (tradeDates.isEmpty()) {
            tradeDates = loadCalendarIfAbsent();
        }
        return tradeClock.resolve(LocalDateTime.now(SHANGHAI), tradeDates);
    }

    /**
     * 刷新交易日历。异常时保留已有日历，不用周末规则猜测节假日。
     *
     * @return 当前可用交易日历
     */
    public List<LocalDate> refreshCalendar() {
        synchronized (calendarRefreshMonitor) {
            return refreshCalendarLocked();
        }
    }

    /**
     * 冷缓存只允许一个线程访问 AKTools；等待线程复用首个线程保存的结果。
     */
    private List<LocalDate> loadCalendarIfAbsent() {
        synchronized (calendarRefreshMonitor) {
            List<LocalDate> tradeDates = snapshotStore.getTradeCalendar();
            if (!tradeDates.isEmpty()) {
                return tradeDates;
            }
            return refreshCalendarLocked();
        }
    }

    /**
     * 调用方必须持有 calendarRefreshMonitor，确保 MiniRacer 接口不会并发执行。
     */
    private List<LocalDate> refreshCalendarLocked() {
        try {
            List<LocalDate> tradeDates = marketClient.fetchTradeDates();
            snapshotStore.saveTradeCalendar(tradeDates);
            return tradeDates;
        } catch (RuntimeException e) {
            LOGGER.warn("交易日历刷新失败，继续使用历史缓存，error={}", e.getMessage());
            return snapshotStore.getTradeCalendar();
        }
    }
}
