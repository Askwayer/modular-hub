package com.vita.market.

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

.entity;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..entity
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股个股资金40秒采样点 实体类
 * @version 1.0
 */
@Data
@TableName("market_stock_fund_point")
@EqualsAndHashCode(callSuper = true)
public class MarketStockFundPoint extends BaseEntity {

    /**
     * 股票编号
     */
    @TableField("stock_code")
    private String stockCode;

    /**
     * 交易日
     */
    @TableField("trade_date")
    private LocalDate tradeDate;

    /**
     * 40秒采样桶起始时间
     */
    @TableField("sample_bucket")
    private LocalDateTime sampleBucket;

    /**
     * 数据源实际采样时间
     */
    @TableField("sample_time")
    private LocalDateTime sampleTime;

    /**
     * 资金流入额，单位人民币元
     */
    @TableField("inflow_amount")
    private BigDecimal inflowAmount;

    /**
     * 资金流出额，单位人民币元且保持正数
     */
    @TableField("outflow_amount")
    private BigDecimal outflowAmount;

    /**
     * 资金净额，单位人民币元
     */
    @TableField("net_amount")
    private BigDecimal netAmount;

    /**
     * 最新价
     */
    @TableField("latest_price")
    private BigDecimal latestPrice;

    /**
     * 涨跌幅，单位百分比
     */
    @TableField("change_percent")
    private BigDecimal changePercent;

    /**
     * 成交额，单位人民币元
     */
    @TableField("turnover_amount")
    private BigDecimal turnoverAmount;

    /**
     * 换手率，单位百分比
     */
    @TableField("turnover_rate")
    private BigDecimal turnoverRate;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
