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
 * @Description: A股市场日汇总 实体类
 * @version 1.0
 */
@Data
@TableName("market_daily_summary")
@EqualsAndHashCode(callSuper = true)
public class MarketDailySummary extends BaseEntity {

    /**
     * 交易日
     */
    @TableField("trade_date")
    private LocalDate tradeDate;

    /**
     * 沪市成交额，单位人民币元
     */
    @TableField("sh_turnover_amount")
    private BigDecimal shTurnoverAmount;

    /**
     * 深市成交额，单位人民币元
     */
    @TableField("sz_turnover_amount")
    private BigDecimal szTurnoverAmount;

    /**
     * 北交所成交额，单位人民币元
     */
    @TableField("bj_turnover_amount")
    private BigDecimal bjTurnoverAmount;

    /**
     * 上涨家数
     */
    @TableField("up_count")
    private Integer upCount;

    /**
     * 下跌家数
     */
    @TableField("down_count")
    private Integer downCount;

    /**
     * 平盘家数
     */
    @TableField("flat_count")
    private Integer flatCount;

    /**
     * 涨停家数
     */
    @TableField("limit_up_count")
    private Integer limitUpCount;

    /**
     * 跌停家数
     */
    @TableField("limit_down_count")
    private Integer limitDownCount;

    /**
     * 主力净流入，单位人民币元
     */
    @TableField("main_net_inflow")
    private BigDecimal mainNetInflow;

    /**
     * 最后有效行情时间
     */
    @TableField("last_valid_time")
    private LocalDateTime lastValidTime;

    /**
     * 是否收盘定稿 1是 0否
     */
    @TableField("is_finalized")
    private Boolean isFinalized;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
