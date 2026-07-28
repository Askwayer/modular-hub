package com.vita.market.support;

import com.vita.market.constant.MarketConstants;
import com.vita.market.enums.MarketDataStatus;
import com.vita.market.model.MarketCollectionResult;
import com.vita.market.model.MarketDataModels;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 九个固定指数的多源采集与确定性合并。
 */
@Component
public class MarketIndexCollector {

    private final AkToolsMarketClient marketClient;
    private final Executor marketSourceTaskExecutor;

    public MarketIndexCollector(AkToolsMarketClient marketClient,
                                @Qualifier("marketSourceTaskExecutor") Executor marketSourceTaskExecutor) {
        this.marketClient = marketClient;
        this.marketSourceTaskExecutor = marketSourceTaskExecutor;
    }

    public MarketCollectionResult<List<MarketDataModels.IndexQuote>> collect(
            List<MarketDataModels.IndexQuote> previous) {
        Map<String, MarketDataModels.IndexSpot> spots;
        boolean spotFailed = false;
        try {
            spots = marketClient.fetchIndexSpots();
        } catch (RuntimeException error) {
            spots = Collections.emptyMap();
            spotFailed = true;
        }

        Map<String, CompletableFuture<List<MarketDataModels.CurvePoint>>> curveTasks = new LinkedHashMap<>();
        Map<String, CompletableFuture<List<MarketDataModels.IndexDailyPoint>>> dailyTasks = new LinkedHashMap<>();
        for (MarketConstants.IndexDefinition definition : MarketConstants.INDEX_DEFINITIONS) {
            curveTasks.put(definition.code(), safeAsync(() -> marketClient.fetchIndexCurve(definition.code())));
            dailyTasks.put(definition.code(), safeAsync(() -> marketClient.fetchIndexDaily(definition.sourceSymbol())));
        }
        CompletableFuture.allOf(
                java.util.stream.Stream.concat(curveTasks.values().stream(), dailyTasks.values().stream())
                        .toArray(CompletableFuture[]::new)).join();

        Map<String, MarketDataModels.IndexQuote> previousByCode = new LinkedHashMap<>();
        if (previous != null) {
            previous.forEach(item -> previousByCode.put(item.indexCode(), item));
        }

        List<MarketDataModels.IndexQuote> result = new ArrayList<>();
        int freshCount = 0;
        int validCount = 0;
        LocalDateTime latestSourceTime = null;
        for (MarketConstants.IndexDefinition definition : MarketConstants.INDEX_DEFINITIONS) {
            MarketDataModels.IndexQuote quote = merge(
                    definition,
                    spots.get(definition.code()),
                    curveTasks.get(definition.code()).join(),
                    dailyTasks.get(definition.code()).join(),
                    previousByCode.get(definition.code()));
            result.add(quote);
            if (quote.dataStatus() == MarketDataStatus.FRESH) {
                freshCount++;
            }
            if (quote.latestValue() != null) {
                validCount++;
            }
            if (quote.sourceDataTime() != null
                    && (latestSourceTime == null || quote.sourceDataTime().isAfter(latestSourceTime))) {
                latestSourceTime = quote.sourceDataTime();
            }
        }

        if (validCount == 0) {
            return new MarketCollectionResult<>(
                    result, MarketDataStatus.ERROR, latestSourceTime, "九个固定指数均无有效数据");
        }
        if (!spotFailed && freshCount == MarketConstants.INDEX_DEFINITIONS.size()) {
            return MarketCollectionResult.fresh(result, latestSourceTime);
        }
        return MarketCollectionResult.stale(result, latestSourceTime, "部分指数使用降级数据或缺少日内曲线");
    }

    private MarketDataModels.IndexQuote merge(MarketConstants.IndexDefinition definition,
                                              MarketDataModels.IndexSpot spot,
                                              List<MarketDataModels.CurvePoint> curve,
                                              List<MarketDataModels.IndexDailyPoint> daily,
                                              MarketDataModels.IndexQuote previous) {
        List<MarketDataModels.CurvePoint> safeCurve = curve == null ? List.of() : curve;
        List<MarketDataModels.IndexDailyPoint> safeDaily = daily == null ? List.of() : daily;
        MarketDataModels.CurvePoint lastCurve = safeCurve.isEmpty() ? null : safeCurve.get(safeCurve.size() - 1);
        MarketDataModels.IndexDailyPoint lastDaily =
                safeDaily.isEmpty() ? null : safeDaily.get(safeDaily.size() - 1);
        MarketDataModels.IndexDailyPoint previousDaily =
                safeDaily.size() < 2 ? null : safeDaily.get(safeDaily.size() - 2);

        BigDecimal latest = spot == null ? null : spot.latestValue();
        BigDecimal changeAmount = spot == null ? null : spot.changeAmount();
        BigDecimal changePercent = spot == null ? null : spot.changePercent();
        BigDecimal turnover = spot == null ? null : spot.turnoverAmount();
        LocalDateTime sourceTime = lastCurve == null ? null : lastCurve.time();
        String source = spot == null ? "NONE" : "SINA";
        boolean stale = spot == null || safeCurve.isEmpty() || safeDaily.isEmpty();

        if (lastCurve != null) {
            source = appendSource(source, "EASTMONEY");
            if (latest == null) {
                latest = lastCurve.value();
            }
        }
        if (latest == null && lastDaily != null) {
            latest = lastDaily.close();
            sourceTime = LocalDateTime.of(lastDaily.tradeDate(), LocalTime.of(15, 0));
            source = "TENCENT";
            stale = true;
        }
        if ((changeAmount == null || changePercent == null)
                && latest != null && previousDaily != null && previousDaily.close() != null
                && previousDaily.close().signum() != 0) {
            changeAmount = latest.subtract(previousDaily.close());
            changePercent = changeAmount
                    .multiply(new BigDecimal("100"))
                    .divide(previousDaily.close(), 4, RoundingMode.HALF_UP);
            source = appendSource(source, "TENCENT");
            stale = true;
        }
        if (latest != null) {
            return new MarketDataModels.IndexQuote(
                    definition.code(), definition.name(), latest, changeAmount, changePercent, turnover,
                    sourceTime, source, stale ? MarketDataStatus.STALE : MarketDataStatus.FRESH, safeCurve);
        }
        if (previous != null && previous.latestValue() != null) {
            return new MarketDataModels.IndexQuote(
                    definition.code(), definition.name(), previous.latestValue(), previous.changeAmount(),
                    previous.changePercent(), previous.turnoverAmount(), previous.sourceDataTime(),
                    appendSource(previous.dataSource(), "CACHE"), MarketDataStatus.STALE, previous.curve());
        }
        return new MarketDataModels.IndexQuote(
                definition.code(), definition.name(), null, null, null, null,
                null, "NONE", MarketDataStatus.ERROR, List.of());
    }

    private String appendSource(String source, String addition) {
        if (source == null || source.isBlank() || "NONE".equals(source)) {
            return addition;
        }
        return source.contains(addition) ? source : source + "+" + addition;
    }

    private <T> CompletableFuture<List<T>> safeAsync(ListSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (RuntimeException error) {
                return List.of();
            }
        }, marketSourceTaskExecutor);
    }

    @FunctionalInterface
    private interface ListSupplier<T> {
        List<T> get();
    }
}
