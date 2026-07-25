package com.vita.market.vo;

import com.vita.market.enums.MarketDataStatus;
import com.vita.market.enums.MarketTradeStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.vo
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 带降级元信息的统一行情响应
 * @Version: 1.0
 */
@Data
public class MarketModuleResult<T> implements Serializable {
    private MarketDataStatus dataStatus;
    private MarketTradeStatus tradeStatus;
    private LocalDate tradeDate;
    private LocalDate recentValidDate;
    private LocalDateTime sourceDataTime;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextRefreshAt;
    private String message;
    private T data;
}
