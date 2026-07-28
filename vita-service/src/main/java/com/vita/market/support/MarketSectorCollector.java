package com.vita.market.support;

import com.vita.market.enums.MarketDataStatus;
import com.vita.market.model.MarketCollectionResult;
import com.vita.market.model.MarketDataModels;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 行业和概念板块多源合并器，只允许代码或标准化后的完整名称精确关联。
 */
@Component
public class MarketSectorCollector {

    private final AkToolsMarketClient marketClient;

    public MarketSectorCollector(AkToolsMarketClient marketClient) {
        this.marketClient = marketClient;
    }

    public MarketCollectionResult<List<MarketDataModels.SectorQuote>> collectSectors() {
        List<MarketDataModels.SectorQuote> result = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        List<MarketDataModels.SectorQuote> industrySources = new ArrayList<>();
        try {
            industrySources.addAll(marketClient.fetchIndustryMarketSectors());
        } catch (RuntimeException error) {
            failures.add("东方财富行业板块");
        }
        try {
            industrySources.addAll(marketClient.fetchIndustrySummarySectors());
        } catch (RuntimeException error) {
            failures.add("同花顺行业汇总");
        }
        if (!industrySources.isEmpty()) {
            result.addAll(merge(industrySources));
        }
        try {
            List<MarketDataModels.SectorQuote> concepts = merge(marketClient.fetchConceptSectors());
            try {
                Map<String, MarketDataModels.SectorRanking> rankingByName = todayConceptRanking();
                concepts = concepts.stream().map(item -> {
                    MarketDataModels.SectorRanking ranking = rankingByName.get(normalizeName(item.name()));
                    return ranking == null || item.mainNetInflow() != null ? item
                            : new MarketDataModels.SectorQuote(
                            item.sectorType(), item.sectorCode(), item.name(), item.changePercent(),
                            null, item.marketCap(), item.turnoverRate(), item.upCount(), item.downCount(),
                            item.leader(), ranking.mainNetInflow());
                }).toList();
            } catch (RuntimeException error) {
                failures.add("概念资金");
            }
            result.addAll(concepts);
        } catch (RuntimeException error) {
            failures.add("概念板块");
        }
        if (result.isEmpty()) {
            return new MarketCollectionResult<>(
                    null, MarketDataStatus.ERROR, null, "行业和概念板块均采集失败");
        }
        if (failures.isEmpty()) {
            return MarketCollectionResult.fresh(result, (LocalDateTime) null);
        }
        return MarketCollectionResult.stale(result, null, String.join("、", failures) + "采集失败");
    }

    public MarketCollectionResult<List<MarketDataModels.SectorRanking>> collectRankings() {
        try {
            return MarketCollectionResult.fresh(marketClient.fetchSectorRankings(), null);
        } catch (RuntimeException error) {
            return new MarketCollectionResult<>(
                    null, MarketDataStatus.ERROR, null, error.getMessage());
        }
    }

    private Map<String, MarketDataModels.SectorRanking> todayConceptRanking() {
        Map<String, MarketDataModels.SectorRanking> result = new LinkedHashMap<>();
        for (MarketDataModels.SectorRanking ranking : marketClient.fetchSectorRankings()) {
            if ("concept".equals(ranking.sectorType()) && "今日".equals(ranking.period())) {
                result.put(normalizeName(ranking.sectorName()), ranking);
            }
        }
        return result;
    }

    private List<MarketDataModels.SectorQuote> merge(List<MarketDataModels.SectorQuote> source) {
        Map<String, MarketDataModels.SectorQuote> merged = new LinkedHashMap<>();
        Map<String, String> keyByName = new LinkedHashMap<>();
        for (MarketDataModels.SectorQuote item : source) {
            String name = normalizeName(item.name());
            String codeKey = item.sectorCode() == null || item.sectorCode().isBlank()
                    ? null : "code:" + item.sectorCode().trim().toUpperCase();
            String key = codeKey != null && merged.containsKey(codeKey)
                    ? codeKey : keyByName.get(name);
            if (key == null) {
                key = codeKey == null ? "name:" + name : codeKey;
            }
            MarketDataModels.SectorQuote existing = merged.get(key);
            merged.put(key, existing == null ? item : combine(existing, item));
            keyByName.put(name, key);
        }
        return new ArrayList<>(merged.values());
    }

    private MarketDataModels.SectorQuote combine(MarketDataModels.SectorQuote primary,
                                                 MarketDataModels.SectorQuote supplement) {
        return new MarketDataModels.SectorQuote(
                primary.sectorType(),
                first(primary.sectorCode(), supplement.sectorCode()),
                primary.name(),
                first(primary.changePercent(), supplement.changePercent()),
                first(primary.turnover(), supplement.turnover()),
                first(primary.marketCap(), supplement.marketCap()),
                first(primary.turnoverRate(), supplement.turnoverRate()),
                first(primary.upCount(), supplement.upCount()),
                first(primary.downCount(), supplement.downCount()),
                first(primary.leader(), supplement.leader()),
                first(primary.mainNetInflow(), supplement.mainNetInflow())
        );
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private <T> T first(T primary, T supplement) {
        return primary == null ? supplement : primary;
    }
}
