package com.vita.market..dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..dto
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股个股资金40秒采样点 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MarketStockFundPointUpdateDto extends MarketStockFundPointCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
