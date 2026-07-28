package com.vita.market.service.impl;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.market.constant.MarketConstants;
import com.vita.market.entity.MarketDailySummary;
import com.vita.market.enums.MarketDataStatus;
import com.vita.market.enums.MarketModule;
import com.vita.market.enums.MarketTradeStatus;
import com.vita.market.model.MarketCollectionResult;
import com.vita.market.model.MarketDataModels;
import com.vita.market.model.MarketModuleState;
import com.vita.market.model.MarketTradeContext;
import com.vita.market.property.MarketProperty;
import com.vita.market.service.IMarketRefreshService;
import com.vita.market.service.IMarketStockService;
import com.vita.market.service.MarketTradeCalendarService;
import com.vita.market.support.*;
import com.vita.redis.RedisCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 行情模块化刷新协调器。
 */
@Service
public class MarketRefreshServiceImpl implements IMarketRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketRefreshServiceImpl.class);
    private static final Set<MarketModule> CLOSE_MODULES = EnumSet.allOf(MarketModule.class);

    private final AkToolsMarketClient marketClient;
    private final MarketIndexCollector indexCollector;
    private final MarketSectorCollector sectorCollector;
    private final MarketFundCollector fundCollector;
    private final MarketSnapshotStore snapshotStore;
    private final MarketTradeCalendarService tradeCalendarService;
    private final IMarketStockService stockService;
    private final RedisCache redisCache;
    private final RedissonClient redissonClient;
    private final MarketProperty property;
    private final Executor marketRefreshTaskExecutor;

    public MarketRefreshServiceImpl(AkToolsMarketClient marketClient,
                                    MarketIndexCollector indexCollector,
                                    MarketSectorCollector sectorCollector,
                                    MarketFundCollector fundCollector,
                                    MarketSnapshotStore snapshotStore,
                                    MarketTradeCalendarService tradeCalendarService,
                                    IMarketStockService stockService,
                                    RedisCache redisCache,
                                    RedissonClient redissonClient,
                                    MarketProperty property,
                                    @Qualifier("marketRefreshTaskExecutor") Executor marketRefreshTaskExecutor) {
        this.marketClient = marketClient;
        this.indexCollector = indexCollector;
        this.sectorCollector = sectorCollector;
        this.fundCollector = fundCollector;
        this.snapshotStore = snapshotStore;
        this.tradeCalendarService = tradeCalendarService;
        this.stockService = stockService;
        this.redisCache = redisCache;
        this.redissonClient = redissonClient;
        this.property = property;
        this.marketRefreshTaskExecutor = marketRefreshTaskExecutor;
    }

    @Override
    public boolean triggerManualRefresh() {
        boolean accepted = redisCache.setIfAbsent(
                MarketConstants.MANUAL_REFRESH_RATE_KEY,
                LocalDateTime.now(MarketConstants.SHANGHAI_ZONE).toString(),
                property.getManualRefreshIntervalSeconds(),
                TimeUnit.SECONDS);
        if (!accepted) {
            throw new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS);
        }
        CompletableFuture.runAsync(this::refreshScheduled, marketRefreshTaskExecutor);
        return true;
    }

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
     * 每个模块独立保存状态；收盘时只重试尚未定稿的模块。
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
            AtomicReference<List<MarketDataModels.MarketFundPoint>> fundReference = new AtomicReference<>();

            if (shouldRefresh(MarketModule.INDICES, tradeDate, finalized)) {
                List<MarketDataModels.IndexQuote> previous =
                        snapshotStore.getSnapshot(MarketModule.INDICES, tradeDate);
                futures.add(refreshCollected(
                        MarketModule.INDICES, tradeDate, finalized, () -> indexCollector.collect(previous)));
            }
            if (shouldRefresh(MarketModule.SUMMARY, tradeDate, finalized)) {
                futures.add(refreshSummary(tradeDate, finalized, summaryReference));
            }
            if (shouldRefresh(MarketModule.SECTORS, tradeDate, finalized)) {
                futures.add(refreshCollected(
                        MarketModule.SECTORS, tradeDate, finalized, sectorCollector::collectSectors));
            }
            if (shouldRefresh(MarketModule.SECTOR_RANKINGS, tradeDate, finalized)) {
                futures.add(refreshCollected(
                        MarketModule.SECTOR_RANKINGS, tradeDate, finalized, sectorCollector::collectRankings));
            }
            if (shouldRefresh(MarketModule.MUTATIONS, tradeDate, finalized)) {
                futures.add(refreshSimple(
                        MarketModule.MUTATIONS, tradeDate, finalized, marketClient::fetchMutations,
                        values -> values.stream()
                                .map(MarketDataModels.MarketMutation::occurredAt)
                                .filter(java.util.Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null)));
            }
            if (shouldRefresh(MarketModule.FUND_TREND, tradeDate, finalized)) {
                futures.add(refreshFundTrend(tradeDate, finalized, fundReference));
            }
            if (shouldRefresh(MarketModule.STOCK_FUNDS, tradeDate, finalized)) {
                futures.add(refreshStockFunds(tradeDate, finalized));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            enrichSummaryMainNetInflow(tradeDate, finalized, summaryReference.get(), fundReference.get());
            return !finalized || CLOSE_MODULES.stream()
                    .allMatch(module -> isModuleFinalized(module, tradeDate));
        } catch (InterruptedException error) {
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
                MarketDataStatus status = summary.totalTurnoverAmount() == null
                        ? MarketDataStatus.STALE : MarketDataStatus.FRESH;
                snapshotStore.saveSnapshot(
                        MarketModule.SUMMARY, tradeDate, summary, status,
                        summary.lastValidTime(),
                        status == MarketDataStatus.STALE ? "沪深京市场成交额覆盖不完整" : null);
                if (status == MarketDataStatus.FRESH) {
                    markModuleFinalized(MarketModule.SUMMARY, tradeDate, finalized);
                }
            } catch (RuntimeException error) {
                markFailure(MarketModule.SUMMARY, tradeDate, error);
            }
        }, marketRefreshTaskExecutor);
    }

    private CompletableFuture<Void> refreshFundTrend(
            LocalDate tradeDate,
            boolean finalized,
            AtomicReference<List<MarketDataModels.MarketFundPoint>> fundReference) {
        return CompletableFuture.runAsync(() -> {
            MarketCollectionResult<List<MarketDataModels.MarketFundPoint>> collected =
                    fundCollector.collect();
            if (collected.dataStatus() == MarketDataStatus.ERROR || collected.data() == null) {
                snapshotStore.markFailure(MarketModule.FUND_TREND, tradeDate, collected.message());
                return;
            }
            fundReference.set(collected.data());
            saveCollected(MarketModule.FUND_TREND, tradeDate, finalized, collected);
        }, marketRefreshTaskExecutor);
    }

    private CompletableFuture<Void> refreshStockFunds(LocalDate tradeDate, boolean finalized) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 全市场即时资金用于板块成分关联，数据库保存逻辑再按启用股票过滤。
                List<MarketDataModels.StockFundSnapshot> points = marketClient.fetchIndividualFundFlow();
                stockService.saveFundPoints(points);
                snapshotStore.saveSuccess(
                        MarketModule.STOCK_FUNDS, tradeDate, points, latestFundSourceTime(points));
                markModuleFinalized(MarketModule.STOCK_FUNDS, tradeDate, finalized);
            } catch (RuntimeException error) {
                markFailure(MarketModule.STOCK_FUNDS, tradeDate, error);
            }
        }, marketRefreshTaskExecutor);
    }

    private <T> CompletableFuture<Void> refreshCollected(
            MarketModule module,
            LocalDate tradeDate,
            boolean finalized,
            Supplier<MarketCollectionResult<T>> supplier) {
        return CompletableFuture.runAsync(() -> {
            try {
                MarketCollectionResult<T> collected = supplier.get();
                if (collected.dataStatus() == MarketDataStatus.ERROR || collected.data() == null) {
                    snapshotStore.markFailure(module, tradeDate, collected.message());
                    return;
                }
                saveCollected(module, tradeDate, finalized, collected);
            } catch (RuntimeException error) {
                markFailure(module, tradeDate, error);
            }
        }, marketRefreshTaskExecutor);
    }

    private <T> void saveCollected(MarketModule module,
                                   LocalDate tradeDate,
                                   boolean finalized,
                                   MarketCollectionResult<T> collected) {
        snapshotStore.saveSnapshot(
                module, tradeDate, collected.data(), collected.dataStatus(),
                collected.sourceDataTime(), collected.message());
        if (collected.dataStatus() == MarketDataStatus.FRESH) {
            markModuleFinalized(module, tradeDate, finalized);
        }
    }

    private <T> CompletableFuture<Void> refreshSimple(
            MarketModule module,
            LocalDate tradeDate,
            boolean finalized,
            Supplier<T> supplier,
            SourceTimeResolver<T> timeResolver) {
        return CompletableFuture.runAsync(() -> {
            try {
                T data = supplier.get();
                snapshotStore.saveSuccess(module, tradeDate, data, timeResolver.resolve(data));
                markModuleFinalized(module, tradeDate, finalized);
            } catch (RuntimeException error) {
                markFailure(module, tradeDate, error);
            }
        }, marketRefreshTaskExecutor);
    }

    private MarketDataModels.DailySummaryData summarize(
            LocalDate tradeDate,
            List<MarketDataModels.StockQuote> quotes,
            boolean finalized) {
        BigDecimal shTurnover = sumTurnover(quotes, "SH");
        BigDecimal szTurnover = sumTurnover(quotes, "SZ");
        BigDecimal bjTurnover = sumTurnover(quotes, "BJ");
        BigDecimal total = shTurnover == null || szTurnover == null || bjTurnover == null
                ? null : shTurnover.add(szTurnover).add(bjTurnover);
        MarketDailySummary previous = stockService.findPreviousDailySummary(tradeDate);
        BigDecimal previousTotal = previous == null ? null : totalTurnover(previous);
        BigDecimal turnoverChange = total == null || previousTotal == null || previousTotal.signum() == 0
                ? null : total.subtract(previousTotal)
                .multiply(new BigDecimal("100"))
                .divide(previousTotal, 4, RoundingMode.HALF_UP);

        int up = 0;
        int down = 0;
        int flat = 0;
        int limitUp = 0;
        int limitDown = 0;
        boolean completeLimitPrices = true;
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
            }
            if (quote.latestPrice() == null
                    || quote.limitUpPrice() == null || quote.limitDownPrice() == null) {
                completeLimitPrices = false;
            } else {
                if (quote.latestPrice().compareTo(quote.limitUpPrice()) >= 0) {
                    limitUp++;
                }
                if (quote.latestPrice().compareTo(quote.limitDownPrice()) <= 0) {
                    limitDown++;
                }
            }
            if (quote.sourceDataTime() != null
                    && (lastValidTime == null || quote.sourceDataTime().isAfter(lastValidTime))) {
                lastValidTime = quote.sourceDataTime();
            }
        }
        return new MarketDataModels.DailySummaryData(
                tradeDate, shTurnover, szTurnover, bjTurnover, total, turnoverChange,
                up, down, flat,
                completeLimitPrices ? limitUp : null,
                completeLimitPrices ? limitDown : null,
                null, lastValidTime, finalized);
    }

    private BigDecimal sumTurnover(List<MarketDataModels.StockQuote> quotes, String market) {
        List<BigDecimal> amounts = quotes.stream()
                .filter(quote -> market.equals(quote.market()))
                .map(MarketDataModels.StockQuote::turnoverAmount)
                .filter(java.util.Objects::nonNull)
                .toList();
        return amounts.isEmpty() ? null : amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalTurnover(MarketDailySummary summary) {
        if (summary.getShTurnoverAmount() == null
                || summary.getSzTurnoverAmount() == null
                || summary.getBjTurnoverAmount() == null) {
            return null;
        }
        return summary.getShTurnoverAmount()
                .add(summary.getSzTurnoverAmount())
                .add(summary.getBjTurnoverAmount());
    }

    private void enrichSummaryMainNetInflow(
            LocalDate tradeDate,
            boolean finalized,
            MarketDataModels.DailySummaryData currentSummary,
            List<MarketDataModels.MarketFundPoint> currentFunds) {
        MarketDataModels.DailySummaryData summary = currentSummary == null
                ? snapshotStore.getSnapshot(MarketModule.SUMMARY, tradeDate) : currentSummary;
        List<MarketDataModels.MarketFundPoint> funds = currentFunds == null
                ? snapshotStore.getSnapshot(MarketModule.FUND_TREND, tradeDate) : currentFunds;
        if (summary == null || funds == null) {
            return;
        }
        BigDecimal mainNetInflow = funds.stream()
                .filter(point -> tradeDate.equals(point.tradeDate()))
                .map(MarketDataModels.MarketFundPoint::mainNetInflow)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (mainNetInflow == null) {
            return;
        }
        MarketDataModels.DailySummaryData enriched = new MarketDataModels.DailySummaryData(
                summary.tradeDate(), summary.shTurnoverAmount(), summary.szTurnoverAmount(),
                summary.bjTurnoverAmount(), summary.totalTurnoverAmount(),
                summary.turnoverChangePercent(), summary.upCount(), summary.downCount(),
                summary.flatCount(), summary.limitUpCount(), summary.limitDownCount(),
                mainNetInflow, summary.lastValidTime(), summary.finalized());
        stockService.saveDailySummary(enriched);
        MarketModuleState state = snapshotStore.getState(MarketModule.SUMMARY);
        MarketDataStatus status = state == null || state.getDataStatus() == MarketDataStatus.FRESH
                ? MarketDataStatus.FRESH : MarketDataStatus.STALE;
        snapshotStore.saveSnapshot(
                MarketModule.SUMMARY, tradeDate, enriched, status,
                enriched.lastValidTime(), state == null ? null : state.getMessage());
        if (status == MarketDataStatus.FRESH) {
            markModuleFinalized(MarketModule.SUMMARY, tradeDate, finalized);
        }
    }

    private LocalDateTime latestFundSourceTime(List<MarketDataModels.StockFundSnapshot> points) {
        return points.stream()
                .map(MarketDataModels.StockFundSnapshot::sourceDataTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private boolean shouldRefresh(MarketModule module, LocalDate tradeDate, boolean finalized) {
        return !finalized || !isModuleFinalized(module, tradeDate);
    }

    private boolean isModuleFinalized(MarketModule module, LocalDate tradeDate) {
        return redisCache.getCacheObject(
                MarketConstants.closeModuleFinalizedKey(tradeDate.toString(), module.name())) != null;
    }

    private void markModuleFinalized(MarketModule module, LocalDate tradeDate, boolean finalized) {
        if (finalized) {
            redisCache.setCacheObject(
                    MarketConstants.closeModuleFinalizedKey(tradeDate.toString(), module.name()),
                    Boolean.TRUE, 2, TimeUnit.DAYS);
        }
    }

    private void markFailure(MarketModule module, LocalDate tradeDate, RuntimeException error) {
        LOGGER.warn("行情模块刷新失败，module={}, tradeDate={}, error={}",
                module, tradeDate, error.getMessage());
        snapshotStore.markFailure(module, tradeDate, error.getMessage());
    }

    @FunctionalInterface
    private interface SourceTimeResolver<T> {
        LocalDateTime resolve(T data);
    }
}
