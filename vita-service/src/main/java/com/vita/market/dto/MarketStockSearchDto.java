package com.vita.market..dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..dto
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股股票字典 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MarketStockSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
