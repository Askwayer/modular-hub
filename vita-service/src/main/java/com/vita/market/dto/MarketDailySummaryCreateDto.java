package com.vita.market.

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

.dto;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..dto
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股市场日汇总 创建 DTO
 * @version 1.0
 */
@Data
public class MarketDailySummaryCreateDto {
    /**
     * 交易日
     */
    private LocalDate tradeDate;

    /**
     * 沪市成交额，单位人民币元
     */
    private java.math.BigDecimal shTurnoverAmount;

    /**
     * 深市成交额，单位人民币元
     */
    private java.math.BigDecimal szTurnoverAmount;

    /**
     * 北交所成交额，单位人民币元
     */
    private java.math.BigDecimal bjTurnoverAmount;

    /**
     * 上涨家数
     */
    private Integer upCount;

    /**
     * 下跌家数
     */
    private Integer downCount;

    /**
     * 平盘家数
     */
    private Integer flatCount;

    /**
     * 涨停家数
     */
    private Integer limitUpCount;

    /**
     * 跌停家数
     */
    private Integer limitDownCount;

    /**
     * 主力净流入，单位人民币元
     */
    private java.math.BigDecimal mainNetInflow;

    /**
     * 最后有效行情时间
     */
    private LocalDateTime lastValidTime;

    /**
     * 是否收盘定稿 1是 0否
     */
    private Boolean isFinalized;

    /**
     * 备注
     */
    private String remark;

}
