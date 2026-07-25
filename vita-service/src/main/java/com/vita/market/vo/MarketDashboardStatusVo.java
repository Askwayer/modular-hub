package com.vita.market.vo;

import com.vita.market.enums.MarketTradeStatus;
import com.vita.market.model.MarketModuleState;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.vo
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 行情大屏交易和模块状态
 * @Version: 1.0
 */
@Data
public class MarketDashboardStatusVo implements Serializable {
    private MarketTradeStatus tradeStatus;
    private LocalDate tradeDate;
    private LocalDate recentValidDate;
    private LocalDateTime lastRefreshAt;
    private LocalDateTime nextRefreshAt;
    private List<MarketModuleState> modules;
}
