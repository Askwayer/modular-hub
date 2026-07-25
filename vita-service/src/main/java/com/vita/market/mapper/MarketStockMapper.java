package com.vita.market.mapper;

import com.vita.market.entity.MarketStock;
import com.vita.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.mapper
 * @Author: znk
 * @CreateTime: 2026-07-25 09:42:04
 * @Description: A股股票字典 Mapper 接口
 * @Version: 1.0
 */
@Mapper
public interface MarketStockMapper extends BaseMapperX<MarketStock> {
}
