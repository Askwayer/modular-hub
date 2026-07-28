package com.vita.market.model;

import com.vita.market.enums.MarketDataStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.market.model
 * @Author: znk
 * @CreateTime: 2026-07-25
 * @Description: 行情采集与展示的稳定数据投影
 * @Version: 1.0
 */
public final class MarketDataModels {

    private MarketDataModels() {
    }

    public record StockDictionaryItem(String stockCode, String stockName, String market,
                                      String listingStatus, LocalDateTime sourceUpdateTime) implements Serializable {
    }

    public record CurvePoint(LocalDateTime time, BigDecimal value) implements Serializable {
    }

    public record StockQuote(String stockCode, String stockName, String market, BigDecimal latestPrice,
                             BigDecimal changePercent, BigDecimal turnoverAmount, BigDecimal turnoverRate,
                             BigDecimal limitUpPrice, BigDecimal limitDownPrice,
                             LocalDateTime sourceDataTime) implements Serializable {
    }

    public record IndexQuote(String indexCode, String indexName, BigDecimal latestValue,
                             BigDecimal changeAmount, BigDecimal changePercent, BigDecimal turnoverAmount,
                             LocalDateTime sourceDataTime, String dataSource,
                             MarketDataStatus dataStatus, List<CurvePoint> curve) implements Serializable {
    }

    public record IndexSpot(String indexCode, BigDecimal latestValue, BigDecimal changeAmount,
                            BigDecimal changePercent, BigDecimal turnoverAmount) implements Serializable {
    }

    public record IndexDailyPoint(LocalDate tradeDate, BigDecimal close) implements Serializable {
    }

    public record SectorQuote(String sectorType, String sectorCode, String name, BigDecimal changePercent,
                              BigDecimal turnover, BigDecimal marketCap, BigDecimal turnoverRate,
                              Integer upCount, Integer downCount, String leader,
                              BigDecimal mainNetInflow) implements Serializable {
    }

    public record SectorHeatmapData(List<String> availableAreaMetrics,
                                    List<SectorQuote> list) implements Serializable {
    }

    public record SectorTopStock(String stockCode, String stockName, BigDecimal latestPrice,
                                 BigDecimal changePercent, BigDecimal turnoverAmount, BigDecimal turnoverRate,
                                 BigDecimal mainNetInflow, BigDecimal mainNetInflowRatio,
                                 Integer rank, boolean monitorEnabled) implements Serializable {
    }

    public record SectorRanking(String period, String sectorType, String sectorCode, String sectorName,
                                BigDecimal changePercent, BigDecimal mainNetInflow) implements Serializable {
    }

    public record MarketMutation(String sectorCode, String sectorName, String direction,
                                 Integer mutationCount, BigDecimal changePercent,
                                 BigDecimal mainNetInflow, String frequentStock,
                                 String description, LocalDateTime occurredAt) implements Serializable {
    }

    public record MarketFundPoint(LocalDate tradeDate, BigDecimal mainNetInflow,
                                  BigDecimal superLargeNetInflow, BigDecimal largeNetInflow,
                                  BigDecimal mediumNetInflow, BigDecimal smallNetInflow) implements Serializable {
    }

    public record FundTrendPoint(LocalDate tradeDate, BigDecimal mainNetInflow,
                                 BigDecimal superLargeNetInflow, BigDecimal largeNetInflow,
                                 BigDecimal mediumNetInflow, BigDecimal smallNetInflow,
                                 String indexCode, BigDecimal indexValue,
                                 BigDecimal indexChangePercent) implements Serializable {
    }

    public record StockFundSnapshot(String stockCode, LocalDate tradeDate, LocalDateTime sampleTime,
                                    BigDecimal inflowAmount, BigDecimal outflowAmount, BigDecimal netAmount,
                                    BigDecimal latestPrice, BigDecimal changePercent,
                                    BigDecimal turnoverAmount, BigDecimal turnoverRate,
                                    LocalDateTime sourceDataTime) implements Serializable {
    }

    public record DailySummaryData(LocalDate tradeDate, BigDecimal shTurnoverAmount,
                                   BigDecimal szTurnoverAmount, BigDecimal bjTurnoverAmount,
                                   BigDecimal totalTurnoverAmount, BigDecimal turnoverChangePercent,
                                   Integer upCount, Integer downCount, Integer flatCount,
                                   Integer limitUpCount, Integer limitDownCount,
                                   BigDecimal mainNetInflow, LocalDateTime lastValidTime,
                                   boolean finalized) implements Serializable {
    }

    public record StockDictionaryView(String stockCode, String stockName, String market,
                                      String listingStatus, boolean monitorEnabled,
                                      LocalDateTime enabledTime, Integer displayOrder,
                                      LocalDateTime sourceUpdateTime) implements Serializable {
    }

    public record StockMonitorItem(StockDictionaryView stock, StockFundSnapshot latest,
                                   List<StockFundSnapshot> curve) implements Serializable {
    }
}
