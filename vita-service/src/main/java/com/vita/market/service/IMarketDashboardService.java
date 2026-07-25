package com.vita.market.service;

import com.vita.market.model.MarketDataModels;
import com.vita.market.vo.MarketDashboardStatusVo;
import com.vita.market.vo.MarketModuleResult;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.service
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: A股行情大屏读取服务
 * @Version: 1.0
 */
public interface IMarketDashboardService {

    MarketDashboardStatusVo status();

    MarketModuleResult<List<MarketDataModels.IndexQuote>> indices();

    MarketModuleResult<MarketDataModels.DailySummaryData> summary();

    MarketModuleResult<List<MarketDataModels.SectorQuote>> heatmap(
            String sectorType, String areaMetric, String direction, String keyword);

    MarketModuleResult<List<MarketDataModels.SectorTopStock>> topStocks(
            String sectorType, String sectorCode, String metric);

    MarketModuleResult<List<MarketDataModels.SectorRanking>> rankings(String period, String sectorType);

    MarketModuleResult<List<MarketDataModels.MarketMutation>> mutations();

    MarketModuleResult<List<MarketDataModels.FundTrendPoint>> fundTrend(Integer days, String indexCode);
}
