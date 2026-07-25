package com.vita.market.support;

import com.vita.market.constant.MarketConstants;
import com.vita.market.enums.MarketDataStatus;
import com.vita.market.enums.MarketModule;
import com.vita.market.enums.MarketTradeStatus;
import com.vita.market.model.MarketModuleState;
import com.vita.market.property.MarketProperty;
import com.vita.market.vo.MarketModuleResult;
import com.vita.redis.RedisCache;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.support
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 行情 Redis 快照和刷新状态仓库
 * @Version: 1.0
 */
@Component
public class MarketSnapshotStore {

    private final RedisCache redisCache;
    private final MarketProperty property;

    public MarketSnapshotStore(RedisCache redisCache, MarketProperty property) {
        this.redisCache = redisCache;
        this.property = property;
    }

    /**
     * 成功数据与状态分开保存，后续失败只改状态，绝不覆盖此快照。
     *
     * @param module         行情模块
     * @param tradeDate      快照交易日
     * @param data           模块数据
     * @param sourceDataTime 数据源时间
     */
    public void saveSuccess(MarketModule module,
                            LocalDate tradeDate,
                            Object data,
                            LocalDateTime sourceDataTime) {
        LocalDateTime now = LocalDateTime.now(MarketConstants.SHANGHAI_ZONE);
        redisCache.setCacheObject(
                MarketConstants.snapshotKey(tradeDate.toString(), module.name()),
                data,
                property.getSnapshotTtlDays(),
                TimeUnit.DAYS);
        MarketModuleState state = new MarketModuleState();
        state.setModule(module.name());
        state.setDataStatus(MarketDataStatus.FRESH);
        state.setTradeDate(tradeDate);
        state.setSourceDataTime(sourceDataTime);
        state.setLastSuccessAt(now);
        state.setLastAttemptAt(now);
        state.setMessage(null);
        saveState(state);
        redisCache.setCacheObject(
                MarketConstants.RECENT_TRADE_DATE_KEY,
                tradeDate,
                property.getSnapshotTtlDays(),
                TimeUnit.DAYS);
    }

    /**
     * 标记模块失败。已有数据降级为 STALE，无历史数据时标记 ERROR。
     *
     * @param module    行情模块
     * @param tradeDate 本次目标交易日
     * @param message   错误摘要
     */
    public void markFailure(MarketModule module, LocalDate tradeDate, String message) {
        MarketModuleState previous = getState(module);
        boolean hasSnapshot = tradeDate != null && getSnapshot(module, tradeDate) != null;
        MarketModuleState state = previous == null ? new MarketModuleState() : previous;
        state.setModule(module.name());
        state.setDataStatus(hasSnapshot ? MarketDataStatus.STALE : MarketDataStatus.ERROR);
        state.setTradeDate(tradeDate);
        state.setLastAttemptAt(LocalDateTime.now(MarketConstants.SHANGHAI_ZONE));
        state.setMessage(message);
        saveState(state);
    }

    public MarketModuleState getState(MarketModule module) {
        return redisCache.getCacheObject(MarketConstants.moduleStateKey(module.name()));
    }

    public List<MarketModuleState> getAllStates() {
        return java.util.Arrays.stream(MarketModule.values())
                .map(module -> {
                    MarketModuleState state = getState(module);
                    return state == null ? emptyState(module) : state;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    public <T> T getSnapshot(MarketModule module, LocalDate tradeDate) {
        if (tradeDate == null) {
            return null;
        }
        return (T) redisCache.getCacheObject(
                MarketConstants.snapshotKey(tradeDate.toString(), module.name()));
    }

    public LocalDate getRecentTradeDate() {
        return redisCache.getCacheObject(MarketConstants.RECENT_TRADE_DATE_KEY);
    }

    public void saveTradeCalendar(List<LocalDate> tradeDates) {
        redisCache.setCacheObject(
                MarketConstants.TRADE_CALENDAR_KEY,
                tradeDates,
                property.getSnapshotTtlDays(),
                TimeUnit.DAYS);
    }

    @SuppressWarnings("unchecked")
    public List<LocalDate> getTradeCalendar() {
        List<LocalDate> dates = redisCache.getCacheObject(MarketConstants.TRADE_CALENDAR_KEY);
        return dates == null ? Collections.emptyList() : dates;
    }

    /**
     * 组合缓存数据和最近刷新状态，形成所有读取接口统一元信息。
     *
     * @param module         行情模块
     * @param tradeStatus    当前交易状态
     * @param tradeDate      当前日期
     * @param effectiveDate  实际读取快照日期
     * @param nextRefreshAt  下次刷新时间
     * @param <T>            数据类型
     * @return 统一行情响应
     */
    public <T> MarketModuleResult<T> buildResult(MarketModule module,
                                                 MarketTradeStatus tradeStatus,
                                                 LocalDate tradeDate,
                                                 LocalDate effectiveDate,
                                                 LocalDateTime nextRefreshAt) {
        T data = getSnapshot(module, effectiveDate);
        MarketModuleState state = getState(module);
        MarketModuleResult<T> result = new MarketModuleResult<>();
        result.setTradeStatus(tradeStatus);
        result.setTradeDate(tradeDate);
        result.setRecentValidDate(effectiveDate);
        result.setNextRefreshAt(nextRefreshAt);
        result.setData(data);
        if (state == null) {
            result.setDataStatus(data == null ? MarketDataStatus.NO_DATA : MarketDataStatus.STALE);
            return result;
        }
        MarketDataStatus status = state.getDataStatus();
        if (data != null && status == MarketDataStatus.ERROR) {
            status = MarketDataStatus.STALE;
        }
        result.setDataStatus(data == null && status == MarketDataStatus.FRESH ? MarketDataStatus.NO_DATA : status);
        result.setSourceDataTime(state.getSourceDataTime());
        result.setLastSuccessAt(state.getLastSuccessAt());
        result.setLastAttemptAt(state.getLastAttemptAt());
        result.setMessage(state.getMessage());
        return result;
    }

    private void saveState(MarketModuleState state) {
        redisCache.setCacheObject(
                MarketConstants.moduleStateKey(state.getModule()),
                state,
                property.getSnapshotTtlDays(),
                TimeUnit.DAYS);
    }

    private MarketModuleState emptyState(MarketModule module) {
        MarketModuleState state = new MarketModuleState();
        state.setModule(module.name());
        state.setDataStatus(MarketDataStatus.NO_DATA);
        return state;
    }
}
