package com.vita.market.model;

import com.vita.market.enums.MarketTradeStatus;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.model
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 当前交易状态及有效行情日期
 * @Version: 1.0
 */
public record MarketTradeContext(MarketTradeStatus tradeStatus,
                                 LocalDate tradeDate,
                                 LocalDate effectiveTradeDate) implements Serializable {
}
