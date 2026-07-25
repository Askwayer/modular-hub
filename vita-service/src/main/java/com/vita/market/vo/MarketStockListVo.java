package com.vita.market..vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.market..vo
 * @Author znk
 * @CreateTime 2026-07-25 09:42:04
 * @Description: A股股票字典 列表 VO
 * @version 1.0
 */
@Data
public class MarketStockListVo {
    /**
     * 股票编号
     */
    private String stockCode;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 所属市场 SH/SZ/BJ
     */
    private String market;

    /**
     * 上市状态 LISTED/SUSPENDED/DELISTED
     */
    private String listingStatus;

    /**
     * 是否启用资金监控 1是 0否
     */
    private Boolean monitorEnabled;

    /**
     * 最近一次启用监控时间
     */
    private LocalDateTime enabledTime;

    /**
     * 监控显示顺序
     */
    private Integer displayOrder;

    /**
     * 数据源更新时间
     */
    private LocalDateTime sourceUpdateTime;

    /**
     * 备注
     */
    private String remark;

}
