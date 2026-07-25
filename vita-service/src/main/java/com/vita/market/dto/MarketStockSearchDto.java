package com.vita.market.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.dto
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 股票字典分页查询条件
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MarketStockSearchDto extends PageRequest {
    private String stockCode;
    private String stockName;
    private String market;
    private String listingStatus;
    private Boolean monitorEnabled;
}
