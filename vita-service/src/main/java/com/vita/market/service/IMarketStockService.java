package com.vita.market.service;

import com.vita.core.page.PageResponse;
import com.vita.market.dto.MarketStockSearchDto;
import com.vita.market.entity.MarketStock;
import com.vita.market.model.MarketDataModels;

import java.time.LocalDate;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.service
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 股票字典、监控配置和采样持久化服务
 * @Version: 1.0
 */
public interface IMarketStockService {

    PageResponse<MarketDataModels.StockDictionaryView> pageDictionary(MarketStockSearchDto searchDto);

    PageResponse<MarketDataModels.StockMonitorItem> pageMonitor(String keyword, Integer pageNum);

    void enable(String stockCode);

    void disable(String stockCode);

    void reorder(List<String> stockCodes);

    int syncDictionary();

    List<MarketStock> listEnabledStocks();

    void saveFundPoints(List<MarketDataModels.StockFundSnapshot> points);

    void saveDailySummary(MarketDataModels.DailySummaryData summary);

    List<MarketDataModels.StockFundSnapshot> listFundPoints(String stockCode, LocalDate tradeDate);
}
