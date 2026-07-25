package com.vita.market.service.impl;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.market.constant.MarketConstants;
import com.vita.market.enums.MarketModule;
import com.vita.market.enums.MarketTradeStatus;
import com.vita.market.model.MarketDataModels;
import com.vita.market.model.MarketTradeContext;
import com.vita.market.property.MarketProperty;
import com.vita.market.service.IMarketRefreshService;
import com.vita.market.service.IMarketStockService;
import com.vita.market.service.MarketTradeCalendarService;
import com.vita.market.support.AkToolsMarketClient;
import com.vita.market.support.MarketSnapshotStore;
import com.vita.redis.RedisCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 行情模块化刷新协调器
 * @Version: 1.0
 */
@Service
public class MarketRefreshServiceImpl implements IMarketRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketRefreshServiceImpl.class);
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final AkToolsMarketClient marketClient;
    private final MarketSnapshotStore snapshotStore;
    private final MarketTradeCalendarService tradeCalendarService;
    private final IMarketStockService stockService;
    private final RedisCache redisCache;
    private final RedissonClient redissonClient;
    private final MarketProperty property;
    private final Executor marketRefreshTaskExecutor;

    public MarketRefreshServiceImpl(AkToolsMarketClient marketClient,
                                    MarketSnapshotStore snapshotStore,
                                    MarketTradeCalendarService tradeCalendarService,
                                    IMarketStockService stockService,
                                    RedisCache redisCache,
                                    RedissonClient redissonClient,
                                    MarketProperty property,
                                    @Qualifier("marketRefreshTaskExecutor") Executor marketRefreshTaskExecutor) {
        this.marketClient = marketClient;
        this.snapshotStore = snapshotStore;
        this.tradeCalendarService = tradeCalendarService;
        this.stockService = stockService;
        this.redisCache = redisCache;
        this.redissonClient = redissonClient;
        this.property = property;
        this.marketRefreshTaskExecutor = marketRefreshTaskExecutor;
    }

    /**
     * 普通用户手动刷新通过 Redis 限频后异步触发，不阻塞大屏请求。
     *
     * @return 是否接受本次刷新
     */
    @Override
    public boolean triggerManualRefresh() {
        boolean accepted = redisCache.setIfAbsent(
                MarketConstants.MANUAL_REFRESH_RATE_KEY,
                LocalDateTime.now(SHANGHAI).toString(),
                property.getManualRefreshIntervalSeconds(),
                TimeUnit.SECONDS);
        if (!accepted) {
            throw new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS);
        }
        CompletableFuture.runAsync(this::refreshScheduled, marketRefreshTaskExecutor);
        return true;
    }

    /**
     * 40秒调度入口。午休和非交易日只维护状态；收盘后仅执行一次定稿。
     */
    @Override
    public void refreshScheduled() {
        MarketTradeContext context = tradeCalendarService.current();
        if (context.tradeStatus() == MarketTradeStatus.TRADING) {
            refreshWithGlobalLock(context.effectiveTradeDate(), false);
            return;
        }
        if (context.tradeStatus() == MarketTradeStatus.CLOSED && context.effectiveTradeDate() != null) {
            String finalizedKey = MarketConstants.CLOSE_FINALIZED_PREFIX + context.effectiveTradeDate();
            if (redisCache.getCacheObject(finalizedKey) == null
                    && refreshWithGlobalLock(context.effectiveTradeDate(), true)) {
                redisCache.setCacheObject(finalizedKey, Boolean.TRUE, 2, TimeUnit.DAYS);
            }
        }
    }

    @Override
    public int syncStockDictionary() {
        return stockService.syncDictionary();
    }

    @Override
    public void refreshTradeCalendar() {
        tradeCalendarService.refreshCalendar();
    }

    /**
     * 全局锁只包围一次刷新周期；每个模块在独立 Future 中自行降级。
     */
    private boolean refreshWithGlobalLock(LocalDate tradeDate, boolean finalized) {
        if (tradeDate == null) {
            return false;
        }
        RLock lock = redissonClient.getLock(MarketConstants.REFRESH_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                LOGGER.debug("行情刷新锁已被其他实例持有，跳过本轮");
                return false;
            }
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            AtomicReference<MarketDataModels.DailySummaryData> summaryReference = new AtomicReference<>();
            AtomicReference<List<MarketDataModels.FundTrendPoint>> fundTrendReference = new AtomicReference<>();
            futures.add(refreshModule(MarketModule.INDICES, tradeDate, marketClient::fetchIndices));
            futures.add(refreshSummary(tradeDate, finalized, summaryReference));
            futures.add(refreshModule(MarketModule.SECTORS, tradeDate, () -> {
                List<MarketDataModels.SectorQuote> sectors = new ArrayList<>();
                sectors.addAll(marketClient.fetchSectorSnapshot("industry"));
                sectors.addAll(marketClient.fetchSectorSnapshot("concept"));
                return sectors;
            }));
            futures.add(refreshModule(MarketModule.SECTOR_RANKINGS, tradeDate, marketClient::fetchSectorRankings));
            futures.add(refreshModule(MarketModule.MUTATIONS, tradeDate, marketClient::fetchMutations));
            futures.add(refreshFundTrend(tradeDate, fundTrendReference));
            futures.add(refreshStockFunds(tradeDate));
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            enrichSummaryMainNetInflow(tradeDate, summaryReference.get(), fundTrendReference.get());
            if (!finalized) {
                return true;
            }
            MarketDataModels.DailySummaryData summary =
                    snapshotStore.getSnapshot(MarketModule.SUMMARY, tradeDate);
            return summary != null && summary.finalized();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private CompletableFuture<Void> refreshSummary(
            LocalDate tradeDate,
            boolean finalized,
            AtomicReference<MarketDataModels.DailySummaryData> summaryReference) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<MarketDataModels.StockQuote> quotes = marketClient.fetchMarketSnapshot();
                MarketDataModels.DailySummaryData summary = summarize(tradeDate, quotes, finalized);
                summaryReference.set(summary);
                stockService.saveDailySummary(summary);
                snapshotStore.saveSuccess(
                        MarketModule.SUMMARY, tradeDate, summary, summary.lastValidTime());
            } catch (RuntimeException e) {
                markFailure(MarketModule.SUMMARY, tradeDate, e);
            }
        }, marketRefreshTaskExecutor);
    }

    private CompletableFuture<Void> refreshFundTrend(
            LocalDate tradeDate,
            AtomicReference<List<MarketDataModels.FundTrendPoint>> fundTrendReference) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<MarketDataModels.FundTrendPoint> trend = marketClient.fetchFundTrend("000300");
                fundTrendReference.set(trend);
                snapshotStore.saveSuccess(
                        MarketModule.FUND_TREND, tradeDate, trend, LocalDateTime.now(SHANGHAI));
            } catch (RuntimeException e) {
                markFailure(MarketModule.FUND_TREND, tradeDate, e);
            }
        }, marketRefreshTaskExecutor);
    }

    private CompletableFuture<Void> refreshStockFunds(LocalDate tradeDate) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (stockService.listEnabledStocks().isEmpty()) {
                    snapshotStore.saveSuccess(
                            MarketModule.STOCK_FUNDS, tradeDate, List.of(), LocalDateTime.now(SHANGHAI));
                    return;
                }
                List<MarketDataModels.StockFundSnapshot> points = marketClient.fetchIndividualFundFlow();
                stockService.saveFundPoints(points);
                snapshotStore.saveSuccess(
                        MarketModule.STOCK_FUNDS, tradeDate, points, latestFundTime(points));
            } catch (RuntimeException e) {
                markFailure(MarketModule.STOCK_FUNDS, tradeDate, e);
            }
        }, marketRefreshTaskExecutor);
    }

    private <T> CompletableFuture<Void> refreshModule(MarketModule module,
                                                       LocalDate tradeDate,
                                                       Supplier<T> supplier) {
        return CompletableFuture.runAsync(() -> {
            try {
                T data = supplier.get();
                snapshotStore.saveSuccess(
                        module, tradeDate, data, LocalDateTime.now(SHANGHAI));
            } catch (RuntimeException e) {
                markFailure(module, tradeDate, e);
            }
        }, marketRefreshTaskExecutor);
    }

    /**
     * 汇总数值只由本次完整市场快照计算，未知值保持 null，不补零。
     */
    private MarketDataModels.DailySummaryData summarize(LocalDate tradeDate,
                                                        List<MarketDataModels.StockQuote> quotes,
                                                        boolean finalized) {
        BigDecimal shTurnover = sumTurnover(quotes, "SH");
        BigDecimal szTurnover = sumTurnover(quotes, "SZ");
        BigDecimal bjTurnover = sumTurnover(quotes, "BJ");
        int up = 0;
        int down = 0;
        int flat = 0;
        int limitUp = 0;
        int limitDown = 0;
        LocalDateTime lastValidTime = null;
        for (MarketDataModels.StockQuote quote : quotes) {
            BigDecimal change = quote.changePercent();
            if (change != null) {
                if (change.signum() > 0) {
                    up++;
                } else if (change.signum() < 0) {
                    down++;
                } else {
                    flat++;
                }
                if (change.compareTo(new BigDecimal("9.9")) >= 0) {
                    limitUp++;
                }
                if (change.compareTo(new BigDecimal("-9.9")) <= 0) {
                    limitDown++;
                }
            }
            if (quote.sourceDataTime() != null
                    && (lastValidTime == null || quote.sourceDataTime().isAfter(lastValidTime))) {
                lastValidTime = quote.sourceDataTime();
            }
        }
        return new MarketDataModels.DailySummaryData(
                tradeDate, shTurnover, szTurnover, bjTurnover,
                up, down, flat, limitUp, limitDown, null,
                lastValidTime == null ? LocalDateTime.now(SHANGHAI) : lastValidTime,
                finalized);
    }

    private BigDecimal sumTurnover(List<MarketDataModels.StockQuote> quotes, String market) {
        List<BigDecimal> amounts = quotes.stream()
                .filter(quote -> market.equals(quote.market()))
                .map(MarketDataModels.StockQuote::turnoverAmount)
                .filter(java.util.Objects::nonNull)
                .toList();
        return amounts.isEmpty() ? null : amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 汇总和资金模块均成功时补齐主力净流入；资金失败时保持 null，不用零值伪造。
     */
    private void enrichSummaryMainNetInflow(
            LocalDate tradeDate,
            MarketDataModels.DailySummaryData summary,
            List<MarketDataModels.FundTrendPoint> fundTrend) {
        if (summary == null || fundTrend == null) {
            return;
        }
        BigDecimal mainNetInflow = fundTrend.stream()
                .filter(point -> tradeDate.equals(point.tradeDate()))
                .map(MarketDataModels.FundTrendPoint::mainNetInflow)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (mainNetInflow == null) {
            return;
        }
        MarketDataModels.DailySummaryData enriched = new MarketDataModels.DailySummaryData(
                summary.tradeDate(),
                summary.shTurnoverAmount(),
                summary.szTurnoverAmount(),
                summary.bjTurnoverAmount(),
                summary.upCount(),
                summary.downCount(),
                summary.flatCount(),
                summary.limitUpCount(),
                summary.limitDownCount(),
                mainNetInflow,
                summary.lastValidTime(),
                summary.finalized());
        stockService.saveDailySummary(enriched);
        snapshotStore.saveSuccess(
                MarketModule.SUMMARY, tradeDate, enriched, enriched.lastValidTime());
    }

    private LocalDateTime latestFundTime(List<MarketDataModels.StockFundSnapshot> points) {
        return points.stream()
                .map(MarketDataModels.StockFundSnapshot::sampleTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now(SHANGHAI));
    }

    private void markFailure(MarketModule module, LocalDate tradeDate, RuntimeException error) {
        LOGGER.warn("行情模块刷新失败，module={}, tradeDate={}, error={}",
                module, tradeDate, error.getMessage());
        snapshotStore.markFailure(module, tradeDate, error.getMessage());
    }
}
