package com.vita.market.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.market.constant.MarketConstants;
import com.vita.market.enums.MarketModule;
import com.vita.market.model.MarketDataModels;
import com.vita.market.model.MarketModuleState;
import com.vita.market.model.MarketTradeContext;
import com.vita.market.property.MarketProperty;
import com.vita.market.service.IMarketDashboardService;
import com.vita.market.service.MarketTradeCalendarService;
import com.vita.market.support.AkToolsMarketClient;
import com.vita.market.support.MarketSnapshotStore;
import com.vita.market.vo.MarketDashboardStatusVo;
import com.vita.market.vo.MarketModuleResult;
import com.vita.redis.RedisCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: A股行情大屏缓存读取与降级服务
 * @Version: 1.0
 */
@Service
public class MarketDashboardServiceImpl implements IMarketDashboardService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final MarketTradeCalendarService tradeCalendarService;
    private final MarketSnapshotStore snapshotStore;
    private final AkToolsMarketClient marketClient;
    private final RedisCache redisCache;
    private final RedissonClient redissonClient;
    private final MarketProperty property;

    public MarketDashboardServiceImpl(MarketTradeCalendarService tradeCalendarService,
                                      MarketSnapshotStore snapshotStore,
                                      AkToolsMarketClient marketClient,
                                      RedisCache redisCache,
                                      RedissonClient redissonClient,
                                      MarketProperty property) {
        this.tradeCalendarService = tradeCalendarService;
        this.snapshotStore = snapshotStore;
        this.marketClient = marketClient;
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
    public MarketModuleResult<List<MarketDataModels.SectorQuote>> heatmap(
            String sectorType, String areaMetric, String direction, String keyword) {
        String normalizedType = normalizeSectorType(sectorType);
        if (!List.of("turnover", "marketCap").contains(defaultValue(areaMetric, "turnover"))) {
            throw badRequest("面积口径仅支持 turnover 或 marketCap");
        }
        String normalizedDirection = defaultValue(direction, "all");
        if (!List.of("all", "up", "down").contains(normalizedDirection)) {
            throw badRequest("方向仅支持 all、up 或 down");
        }
        MarketModuleResult<List<MarketDataModels.SectorQuote>> result = result(MarketModule.SECTORS);
        if (result.getData() == null) {
            return result;
        }
        String filterKeyword = CharSequenceUtil.blankToDefault(keyword, "").trim().toLowerCase(Locale.ROOT);
        result.setData(result.getData().stream()
                .filter(item -> normalizedType.equals(item.sectorType()))
                .filter(item -> filterKeyword.isEmpty()
                        || item.name().toLowerCase(Locale.ROOT).contains(filterKeyword)
                        || item.sectorCode().toLowerCase(Locale.ROOT).contains(filterKeyword))
                .filter(item -> "all".equals(normalizedDirection)
                        || ("up".equals(normalizedDirection) && item.changePercent() != null
                        && item.changePercent().signum() > 0)
                        || ("down".equals(normalizedDirection) && item.changePercent() != null
                        && item.changePercent().signum() < 0))
                .toList());
        return result;
    }

    /**
     * 板块成分按板块独立缓存；缓存缺失时在细粒度锁内拉取，失败沿用旧值。
     */
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
        MarketModuleResult<List<MarketDataModels.SectorTopStock>> result =
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
            data = loadSectorTopStocks(key, normalizedType, sectorCode, normalizedMetric);
        }
        result.setData(data);
        return result;
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.SectorRanking>> rankings(String period, String sectorType) {
        MarketModuleResult<List<MarketDataModels.SectorRanking>> result = result(MarketModule.SECTOR_RANKINGS);
        if (result.getData() == null) {
            return result;
        }
        String normalizedPeriod = normalizePeriod(period);
        String normalizedType = normalizeSectorType(sectorType);
        result.setData(result.getData().stream()
                .filter(item -> normalizedPeriod.equals(item.period()))
                .filter(item -> normalizedType.equals(item.sectorType()))
                .toList());
        return result;
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.MarketMutation>> mutations() {
        return result(MarketModule.MUTATIONS);
    }

    @Override
    public MarketModuleResult<List<MarketDataModels.FundTrendPoint>> fundTrend(Integer days, String indexCode) {
        int normalizedDays = days == null ? 20 : days;
        if (!List.of(5, 10, 20).contains(normalizedDays)) {
            throw badRequest("资金趋势仅支持5、10、20日");
        }
        String normalizedIndex = CharSequenceUtil.blankToDefault(indexCode, "000300").trim();
        if (!MarketConstants.INDEX_NAMES.containsKey(normalizedIndex)) {
            throw badRequest("不支持的指数代码");
        }
        MarketModuleResult<List<MarketDataModels.FundTrendPoint>> result = result(MarketModule.FUND_TREND);
        List<MarketDataModels.FundTrendPoint> data = result.getData();
        if (!"000300".equals(normalizedIndex)) {
            data = loadIndexFundTrend(normalizedIndex);
        }
        if (data != null && data.size() > normalizedDays) {
            data = data.subList(data.size() - normalizedDays, data.size());
        }
        result.setData(data);
        return result;
    }

    private <T> MarketModuleResult<T> result(MarketModule module) {
        MarketTradeContext context = tradeCalendarService.current();
        LocalDate effectiveDate = resolveEffectiveDate(context, module);
        return snapshotStore.buildResult(
                module,
                context.tradeStatus(),
                context.tradeDate(),
                effectiveDate,
                nextRefreshAt(context));
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
        LocalDateTime now = LocalDateTime.now(SHANGHAI);
        return switch (context.tradeStatus()) {
            case TRADING -> now.plusSeconds(MarketConstants.SAMPLE_BUCKET_SECONDS);
            case PRE_OPEN -> LocalDateTime.of(context.tradeDate(), LocalTime.of(9, 30));
            case LUNCH_BREAK -> LocalDateTime.of(context.tradeDate(), LocalTime.of(13, 0));
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private List<MarketDataModels.SectorTopStock> loadSectorTopStocks(String key,
                                                                      String sectorType,
                                                                      String sectorCode,
                                                                      String metric) {
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
            List<MarketDataModels.SectorTopStock> data =
                    marketClient.fetchSectorTopStocks(sectorType, sectorCode, metric);
            redisCache.setCacheObject(key, data, property.getSnapshotTtlDays(), TimeUnit.DAYS);
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<MarketDataModels.FundTrendPoint> loadIndexFundTrend(String indexCode) {
        MarketTradeContext context = tradeCalendarService.current();
        String key = "vita:market:fund-trend:" + context.effectiveTradeDate() + ":" + indexCode;
        List<MarketDataModels.FundTrendPoint> cached = redisCache.getCacheObject(key);
        if (cached != null) {
            return cached;
        }
        List<MarketDataModels.FundTrendPoint> data = marketClient.fetchFundTrend(indexCode);
        redisCache.setCacheObject(key, data, property.getSnapshotTtlDays(), TimeUnit.DAYS);
        return data;
    }

    private String normalizeSectorType(String sectorType) {
        String value = defaultValue(sectorType, "industry");
        if (!List.of("industry", "concept").contains(value)) {
            throw badRequest("板块类型仅支持 industry 或 concept");
        }
        return value;
    }

    private String normalizePeriod(String period) {
        String value = defaultValue(period, "今日");
        return switch (value) {
            case "today" -> "今日";
            case "5d" -> "5日";
            case "10d" -> "10日";
            case "今日", "5日", "10日" -> value;
            default -> throw badRequest("排行周期仅支持今日、5日或10日");
        };
    }

    private String defaultValue(String value, String defaultValue) {
        return CharSequenceUtil.blankToDefault(value, defaultValue).trim();
    }

    private ServiceException badRequest(String message) {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }
}
