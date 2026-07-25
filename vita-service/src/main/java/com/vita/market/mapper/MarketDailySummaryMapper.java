package com.vita.market.mapper;

import com.vita.market.entity.MarketDailySummary;
import com.vita.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.mapper
 * @Author: znk
 * @CreateTime: 2026-07-25 09:42:04
 * @Description: A股市场日汇总 Mapper 接口
 * @Version: 1.0
 */
@Mapper
public interface MarketDailySummaryMapper extends BaseMapperX<MarketDailySummary> {
}
