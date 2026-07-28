package com.vita.market.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.market.constant.MarketConstants;
import com.vita.market.enums.MarketDataStatus;
import com.vita.market.enums.MarketModule;
import com.vita.market.model.MarketDataModels;
import com.vita.market.model.MarketModuleState;
import com.vita.market.model.MarketTradeContext;
import com.vita.market.property.MarketProperty;
import com.vita.market.service.IMarketDashboardService;
import com.vita.market.service.IMarketStockService;
import com.vita.market.service.MarketTradeCalendarService;
import com.vita.market.support.AkToolsMarketClient;
import com.vita.market.support.MarketSnapshotStore;
import com.vita.market.vo.MarketDashboardStatusVo;
import com.vita.market.vo.MarketModuleResult;
import com.vita.redis.RedisCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A股行情大屏缓存读取、筛选和按需关联服务。
 */
@Service
public class MarketDashboardServiceImpl implements IMarketDashboardService {

    private final MarketTradeCalendarService tradeCalendarService;
    private final MarketSnapshotStore snapshotStore;
    private final AkToolsMarketClient marketClient;
    private final IMarketStockService stockService;
    private final RedisCache redisCache;
    private final RedissonClient redissonClient;
    private final MarketProperty property;

    public MarketDashboardServiceImpl(MarketTradeCalendarService tradeCalendarService,
                                      MarketSnapshotStore snapshotStore,
                                      AkToolsMarketClient marketClient,
                                      IMarketStockService stockService,
                                      RedisCache redisCache,
                                      RedissonClient redissonClient,
                                      MarketProperty property) {
        this.tradeCalendarService = tradeCalendarService;
        this.snapshotStore = snapshotStore;
        this.marketClient = marketClient;
        this.stockService = stockService;
        this.redisCache = redisCache;
        this.redissonClient = redissonClient;
        this.property = property;
    }

    @Override
    public MarketDashboardStatusVo status() {
        MarketTradeContext context = tradeCalendarService.current();
        List<MarketModuleState> states = snapshotStore.getAllStates();
        MarketDashboardStatusVo result = new MarketDashboardStatusVo();
        result.setTradeStatus(context.tradeStatus());
        result.setTradeDate(context.tradeDate());
        result.setRecentValidDate(resolveEffectiveDate(context, MarketModule.SUMMARY));
        result.setLastRefreshAt(states.stream()
                .map(MarketModuleState::getLastAttemptAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        result.setNextRefreshAt(nextRefreshAt(context));
        result.setModules(states);
        return result;
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.IndexQuote>> indices() {
        return result(MarketModule.INDICES);
    }

    @Override
    public MarketModuleResult<MarketDataModels.DailySummaryData> summary() {
        return result(MarketModule.SUMMARY);
    }

    @Override
    public MarketModuleResult<MarketDataModels.SectorHeatmapData> heatmap(
            String sectorType, String areaMetric, String direction, String keyword) {
        String normalizedType = normalizeSectorType(sectorType);
        String normalizedMetric = normalizeAreaMetric(areaMetric, normalizedType);
        String normalizedDirection = defaultValue(direction, "all").toLowerCase(Locale.ROOT);
        if (!List.of("all", "up", "down").contains(normalizedDirection)) {
            throw badRequest("方向仅支持 all、up 或 down");
        }
        MarketModuleResult<List<MarketDataModels.SectorQuote>> source = result(MarketModule.SECTORS);
        MarketModuleResult<MarketDataModels.SectorHeatmapData> response = copyMetadata(source);
        List<String> availableMetrics = "concept".equals(normalizedType)
                ? List.of("marketCap") : List.of("turnover", "marketCap");
        if (source.getData() == null) {
            response.setData(new MarketDataModels.SectorHeatmapData(availableMetrics, List.of()));
            return response;
        }
        String normalizedKeyword = CharSequenceUtil.blankToDefault(keyword, "")
                .trim()
                .toLowerCase(Locale.ROOT);
        List<MarketDataModels.SectorQuote> items = source.getData().stream()
                .filter(item -> normalizedType.equals(item.sectorType()))
                .filter(item -> normalizedKeyword.isEmpty()
                        || item.name().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || item.sectorCode() != null
                        && item.sectorCode().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(item -> "all".equals(normalizedDirection)
                        || ("up".equals(normalizedDirection)
                        && item.changePercent() != null && item.changePercent().signum() > 0)
                        || ("down".equals(normalizedDirection)
                        && item.changePercent() != null && item.changePercent().signum() < 0))
                .toList();
        response.setData(new MarketDataModels.SectorHeatmapData(availableMetrics, items));
        return response;
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.SectorTopStock>> topStocks(
            String sectorType, String sectorCode, String metric) {
        String normalizedType = normalizeSectorType(sectorType);
        if (CharSequenceUtil.isBlank(sectorCode)) {
            throw badRequest("板块代码不能为空");
        }
        String normalizedMetric = defaultValue(metric, "changePercent");
        if (!List.of("changePercent", "mainNetInflow", "turnover").contains(normalizedMetric)) {
            throw badRequest("排行口径不支持");
        }
        MarketTradeContext context = tradeCalendarService.current();
        LocalDate effectiveDate = resolveEffectiveDate(context, MarketModule.SECTORS);
        MarketModuleResult<List<MarketDataModels.SectorTopStock>> response =
                snapshotStore.buildResult(
                        MarketModule.SECTORS,
                        context.tradeStatus(),
                        context.tradeDate(),
                        effectiveDate,
                        nextRefreshAt(context));
        String key = MarketConstants.sectorTopStocksKey(
                String.valueOf(effectiveDate), normalizedType, sectorCode, normalizedMetric);
        List<MarketDataModels.SectorTopStock> data = redisCache.getCacheObject(key);
        if (data == null) {
            data = loadSectorTopStocks(key, normalizedType, sectorCode, normalizedMetric, effectiveDate);
        }
        response.setData(data);
        return response;
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.SectorRanking>> rankings(
            String period, String sectorType) {
        MarketModuleResult<List<MarketDataModels.SectorRanking>> response =
                result(MarketModule.SECTOR_RANKINGS);
        if (response.getData() == null) {
            return response;
        }
        String normalizedPeriod = normalizePeriod(period);
        String normalizedType = normalizeSectorType(sectorType);
        response.setData(response.getData().stream()
                .filter(item -> normalizedPeriod.equals(item.period()))
                .filter(item -> normalizedType.equals(item.sectorType()))
                .toList());
        return response;
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.MarketMutation>> mutations() {
        MarketModuleResult<List<MarketDataModels.MarketMutation>> response = result(MarketModule.MUTATIONS);
        if (response.getData() != null && response.getData().size() > 5) {
            response.setData(response.getData().subList(0, 5));
        }
        return response;
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.FundTrendPoint>> fundTrend(
            Integer days, String indexCode) {
        int normalizedDays = days == null ? 20 : days;
        if (!List.of(5, 10, 20).contains(normalizedDays)) {
            throw badRequest("资金趋势仅支持5、10、20日");
        }
        String normalizedIndex = CharSequenceUtil.blankToDefault(indexCode, "000001").trim();
        MarketConstants.IndexDefinition definition = MarketConstants.INDEX_DEFINITIONS.stream()
                .filter(item -> item.code().equals(normalizedIndex))
                .findFirst()
                .orElseThrow(() -> badRequest("不支持的指数代码"));

        MarketModuleResult<List<MarketDataModels.MarketFundPoint>> source = result(MarketModule.FUND_TREND);
        MarketModuleResult<List<MarketDataModels.FundTrendPoint>> response = copyMetadata(source);
        if (source.getData() == null) {
            response.setData(null);
            return response;
        }
        try {
            List<MarketDataModels.FundTrendPoint> enriched =
                    loadIndexFundTrend(source.getData(), definition);
            response.setData(tail(enriched, normalizedDays));
        } catch (RuntimeException error) {
            response.setData(tail(source.getData().stream()
                    .map(item -> new MarketDataModels.FundTrendPoint(
                            item.tradeDate(), item.mainNetInflow(), item.superLargeNetInflow(),
                            item.largeNetInflow(), item.mediumNetInflow(), item.smallNetInflow(),
                            normalizedIndex, null, null))
                    .toList(), normalizedDays));
            response.setDataStatus(MarketDataStatus.STALE);
            response.setMessage("指数对照线获取失败，市场资金数据继续可用");
        }
        return response;
    }

    private List<MarketDataModels.SectorTopStock> loadSectorTopStocks(
            String key, String sectorType, String sectorCode, String metric, LocalDate effectiveDate) {
        RLock lock = redissonClient.getLock("vita:market:lock:sector-top:" + sectorType + ":" + sectorCode);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, property.getRefreshLockLeaseSeconds(), TimeUnit.SECONDS);
            if (!locked) {
                return redisCache.getCacheObject(key);
            }
            List<MarketDataModels.SectorTopStock> cached = redisCache.getCacheObject(key);
            if (cached != null) {
                return cached;
            }
            List<MarketDataModels.SectorTopStock> constituents =
                    marketClient.fetchSectorConstituents(sectorType, sectorCode);
            List<MarketDataModels.StockFundSnapshot> funds =
                    snapshotStore.getSnapshot(MarketModule.STOCK_FUNDS, effectiveDate);
            Map<String, MarketDataModels.StockFundSnapshot> fundByCode = new HashMap<>();
            if (funds != null) {
                funds.forEach(item -> fundByCode.put(item.stockCode(), item));
            }
            Set<String> enabledCodes = new HashSet<>();
            stockService.listEnabledStocks().forEach(item -> enabledCodes.add(item.getStockCode()));
            List<MarketDataModels.SectorTopStock> enriched = constituents.stream()
                    .map(item -> enrichTopStock(item, fundByCode.get(item.stockCode()), enabledCodes))
                    .sorted(topStockComparator(metric))
                    .limit(5)
                    .toList();
            List<MarketDataModels.SectorTopStock> ranked = new ArrayList<>();
            for (int index = 0; index < enriched.size(); index++) {
                MarketDataModels.SectorTopStock item = enriched.get(index);
                ranked.add(new MarketDataModels.SectorTopStock(
                        item.stockCode(), item.stockName(), item.latestPrice(), item.changePercent(),
                        item.turnoverAmount(), item.turnoverRate(), item.mainNetInflow(),
                        item.mainNetInflowRatio(), index + 1, item.monitorEnabled()));
            }
            redisCache.setCacheObject(
                    key, ranked, MarketConstants.SAMPLE_BUCKET_SECONDS, TimeUnit.SECONDS);
            return ranked;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private MarketDataModels.SectorTopStock enrichTopStock(
            MarketDataModels.SectorTopStock item,
            MarketDataModels.StockFundSnapshot fund,
            Set<String> enabledCodes) {
        BigDecimal turnover = fund == null || fund.turnoverAmount() == null
                ? item.turnoverAmount() : fund.turnoverAmount();
        BigDecimal mainNet = fund == null ? null : fund.netAmount();
        BigDecimal ratio = mainNet == null || turnover == null || turnover.signum() == 0
                ? null : mainNet.multiply(new BigDecimal("100"))
                .divide(turnover, 4, RoundingMode.HALF_UP);
        return new MarketDataModels.SectorTopStock(
                item.stockCode(),
                item.stockName(),
                fund == null || fund.latestPrice() == null ? item.latestPrice() : fund.latestPrice(),
                fund == null || fund.changePercent() == null ? item.changePercent() : fund.changePercent(),
                turnover,
                fund == null || fund.turnoverRate() == null ? item.turnoverRate() : fund.turnoverRate(),
                mainNet,
                ratio,
                null,
                enabledCodes.contains(item.stockCode())
        );
    }

    private Comparator<MarketDataModels.SectorTopStock> topStockComparator(String metric) {
        Comparator<BigDecimal> values = Comparator.nullsLast(Comparator.reverseOrder());
        return switch (metric) {
            case "mainNetInflow" -> Comparator.comparing(
                    MarketDataModels.SectorTopStock::mainNetInflow, values);
            case "turnover" -> Comparator.comparing(
                    MarketDataModels.SectorTopStock::turnoverAmount, values);
            default -> Comparator.comparing(
                    MarketDataModels.SectorTopStock::changePercent, values);
        };
    }

    private List<MarketDataModels.FundTrendPoint> loadIndexFundTrend(
            List<MarketDataModels.MarketFundPoint> funds,
            MarketConstants.IndexDefinition definition) {
        MarketTradeContext context = tradeCalendarService.current();
        String key = "vita:market:fund-trend:" + context.effectiveTradeDate() + ":" + definition.code();
        List<MarketDataModels.FundTrendPoint> cached = redisCache.getCacheObject(key);
        if (cached != null) {
            return cached;
        }
        List<MarketDataModels.IndexDailyPoint> indexHistory =
                marketClient.fetchIndexDaily(definition.sourceSymbol());
        Map<LocalDate, MarketDataModels.IndexDailyPoint> indexByDate = new HashMap<>();
        Map<LocalDate, BigDecimal> previousClose = new HashMap<>();
        MarketDataModels.IndexDailyPoint previous = null;
        for (MarketDataModels.IndexDailyPoint point : indexHistory) {
            indexByDate.put(point.tradeDate(), point);
            previousClose.put(point.tradeDate(), previous == null ? null : previous.close());
            previous = point;
        }
        List<MarketDataModels.FundTrendPoint> result = funds.stream().map(item -> {
            MarketDataModels.IndexDailyPoint index = indexByDate.get(item.tradeDate());
            BigDecimal base = previousClose.get(item.tradeDate());
            BigDecimal changePercent = index == null || base == null || base.signum() == 0
                    ? null : index.close().subtract(base)
                    .multiply(new BigDecimal("100"))
                    .divide(base, 4, RoundingMode.HALF_UP);
            return new MarketDataModels.FundTrendPoint(
                    item.tradeDate(), item.mainNetInflow(), item.superLargeNetInflow(),
                    item.largeNetInflow(), item.mediumNetInflow(), item.smallNetInflow(),
                    definition.code(), index == null ? null : index.close(), changePercent);
        }).toList();
        redisCache.setCacheObject(
                key, result, MarketConstants.SAMPLE_BUCKET_SECONDS, TimeUnit.SECONDS);
        return result;
    }

    private <T> List<T> tail(List<T> values, int size) {
        return values.size() <= size ? values : values.subList(values.size() - size, values.size());
    }

    private <T> MarketModuleResult<T> result(MarketModule module) {
        MarketTradeContext context = tradeCalendarService.current();
        LocalDate effectiveDate = resolveEffectiveDate(context, module);
        return snapshotStore.buildResult(
                module, context.tradeStatus(), context.tradeDate(), effectiveDate, nextRefreshAt(context));
    }

    private LocalDate resolveEffectiveDate(MarketTradeContext context, MarketModule module) {
        LocalDate effectiveDate = context.effectiveTradeDate();
        if (effectiveDate == null || snapshotStore.getSnapshot(module, effectiveDate) == null) {
            LocalDate recent = snapshotStore.getRecentTradeDate();
            if (recent != null) {
                effectiveDate = recent;
            }
        }
        return effectiveDate;
    }

    private LocalDateTime nextRefreshAt(MarketTradeContext context) {
        LocalDateTime now = LocalDateTime.now(MarketConstants.SHANGHAI_ZONE);
        return switch (context.tradeStatus()) {
            case TRADING -> now.plusSeconds(MarketConstants.SAMPLE_BUCKET_SECONDS);
            case PRE_OPEN -> LocalDateTime.of(context.tradeDate(), LocalTime.of(9, 30));
            case LUNCH_BREAK -> LocalDateTime.of(context.tradeDate(), LocalTime.of(13, 0));
            default -> null;
        };
    }

    private String normalizeSectorType(String sectorType) {
        String value = defaultValue(sectorType, "industry").toLowerCase(Locale.ROOT);
        if (!List.of("industry", "concept").contains(value)) {
            throw badRequest("板块类型仅支持 industry 或 concept");
        }
        return value;
    }

    private String normalizeAreaMetric(String areaMetric, String sectorType) {
        String fallback = "concept".equals(sectorType) ? "marketCap" : "turnover";
        String raw = defaultValue(areaMetric, fallback);
        String value = switch (raw.toLowerCase(Locale.ROOT)) {
            case "turnover" -> "turnover";
            case "marketcap" -> "marketCap";
            default -> throw badRequest("面积口径仅支持 turnover 或 marketCap");
        };
        if ("concept".equals(sectorType) && "turnover".equals(value)) {
            throw badRequest("概念板块仅支持 marketCap 面积口径");
        }
        return value;
    }

    private String normalizePeriod(String period) {
        String value = defaultValue(period, "今日").toLowerCase(Locale.ROOT);
        return switch (value) {
            case "today", "今日" -> "今日";
            case "5d", "5日" -> "5日";
            case "10d", "10日" -> "10日";
            default -> throw badRequest("排行周期仅支持今日、5日或10日");
        };
    }

    private String defaultValue(String value, String fallback) {
        return CharSequenceUtil.blankToDefault(value, fallback).trim();
    }

    private <T, S> MarketModuleResult<T> copyMetadata(MarketModuleResult<S> source) {
        MarketModuleResult<T> target = new MarketModuleResult<>();
        target.setDataStatus(source.getDataStatus());
        target.setTradeStatus(source.getTradeStatus());
        target.setTradeDate(source.getTradeDate());
        target.setRecentValidDate(source.getRecentValidDate());
        target.setSourceDataTime(source.getSourceDataTime());
        target.setLastSuccessAt(source.getLastSuccessAt());
        target.setLastAttemptAt(source.getLastAttemptAt());
        target.setNextRefreshAt(source.getNextRefreshAt());
        target.setMessage(source.getMessage());
        return target;
    }

    private ServiceException badRequest(String message) {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }
}
