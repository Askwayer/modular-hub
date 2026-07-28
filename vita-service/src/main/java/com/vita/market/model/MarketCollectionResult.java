package com.vita.market.model;

import com.vita.market.enums.MarketDataStatus;

import java.time.LocalDateTime;

/**
 * 行情采集层内部结果，明确区分有效降级数据和完全失败。
 *
 * @param data           标准化后的业务数据
 * @param dataStatus     本轮数据状态
 * @param sourceDataTime 数据源时间
 * @param message        降级或错误摘要
 * @param <T>            数据类型
 */
public record MarketCollectionResult<T>(T data,
                                        MarketDataStatus dataStatus,
                                        LocalDateTime sourceDataTime,
                                        String message) {

    public static <T> MarketCollectionResult<T> fresh(T data, LocalDateTime sourceDataTime) {
        return new MarketCollectionResult<>(data, MarketDataStatus.FRESH, sourceDataTime, null);
    }

    public static <T> MarketCollectionResult<T> stale(T data,
                                                       LocalDateTime sourceDataTime,
                                                       String message) {
        return new MarketCollectionResult<>(data, MarketDataStatus.STALE, sourceDataTime, message);
    }
}
