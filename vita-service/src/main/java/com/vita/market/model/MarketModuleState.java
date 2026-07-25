package com.vita.market.model;

import com.vita.market.enums.MarketDataStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.model
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 单个行情模块刷新状态
 * @Version: 1.0
 */
@Data
public class MarketModuleState implements Serializable {
    private String module;
    private MarketDataStatus dataStatus;
    private LocalDate tradeDate;
    private LocalDateTime sourceDataTime;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastAttemptAt;
    private String message;
}
