package com.vita.market.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.dto
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 股票启停请求
 * @Version: 1.0
 */
@Data
public class MarketStockCodeRequest {

    @NotBlank(message = "股票编号不能为空")
    private String stockCode;
}
