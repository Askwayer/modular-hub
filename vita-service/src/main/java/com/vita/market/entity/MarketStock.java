package com.vita.market.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.entity
 * @Author: znk
 * @CreateTime: 2026-07-25 09:42:04
 * @Description: A股股票字典 实体类
 * @Version: 1.0
 */
@Data
@TableName("market_stock")
@EqualsAndHashCode(callSuper = true)
public class MarketStock extends BaseEntity {

    /**
     * 股票编号
     */
    @TableField("stock_code")
    private String stockCode;

    /**
     * 股票名称
     */
    @TableField("stock_name")
    private String stockName;

    /**
     * 所属市场 SH/SZ/BJ
     */
    @TableField("market")
    private String market;

    /**
     * 上市状态 LISTED/SUSPENDED/DELISTED
     */
    @TableField("listing_status")
    private String listingStatus;

    /**
     * 是否启用资金监控 1是 0否
     */
    @TableField("monitor_enabled")
    private Boolean monitorEnabled;

    /**
     * 最近一次启用监控时间
     */
    @TableField("enabled_time")
    private LocalDateTime enabledTime;

    /**
     * 监控显示顺序
     */
    @TableField("display_order")
    private Integer displayOrder;

    /**
     * 数据源更新时间
     */
    @TableField("source_update_time")
    private LocalDateTime sourceUpdateTime;

}
