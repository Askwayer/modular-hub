package com.vita.market.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.dto
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 启用股票完整排序请求
 * @Version: 1.0
 */
@Data
public class MarketStockReorderRequest {

    @NotEmpty(message = "股票排序不能为空")
    @Size(max = 10, message = "最多允许排序10只股票")
    private List<String> stockCodes;
}
