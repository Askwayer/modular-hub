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
 * @Description: A股个股资金40秒采样点 创建 DTO
 * @version 1.0
 */
@Data
public class MarketStockFundPointCreateDto {
    /**
     * 股票编号
     */
    private String stockCode;

    /**
     * 交易日
     */
    private LocalDate tradeDate;

    /**
     * 40秒采样桶起始时间
     */
    private LocalDateTime sampleBucket;

    /**
     * 数据源实际采样时间
     */
    private LocalDateTime sampleTime;

    /**
     * 资金流入额，单位人民币元
     */
    private java.math.BigDecimal inflowAmount;

    /**
     * 资金流出额，单位人民币元且保持正数
     */
    private java.math.BigDecimal outflowAmount;

    /**
     * 资金净额，单位人民币元
     */
    private java.math.BigDecimal netAmount;

    /**
     * 最新价
     */
    private java.math.BigDecimal latestPrice;

    /**
     * 涨跌幅，单位百分比
     */
    private java.math.BigDecimal changePercent;

    /**
     * 成交额，单位人民币元
     */
    private java.math.BigDecimal turnoverAmount;

    /**
     * 换手率，单位百分比
     */
    private java.math.BigDecimal turnoverRate;

    /**
     * 备注
     */
    private String remark;

}
