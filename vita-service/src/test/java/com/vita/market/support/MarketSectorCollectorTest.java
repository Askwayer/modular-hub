package com.vita.market.support;

import com.vita.market.enums.MarketDataStatus;
import com.vita.market.model.MarketDataModels;
import com.vita.market.property.MarketProperty;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSectorCollectorTest {

    @Test
    void shouldMergeIndustryByExactNormalizedNameAndKeepConceptTurnoverNull() {
        MarketSectorCollector collector = new MarketSectorCollector(new TestMarketClient());

        var collected = collector.collectSectors();

        assertThat(collected.dataStatus()).isEqualTo(MarketDataStatus.FRESH);
        assertThat(collected.data()).hasSize(2);
        MarketDataModels.SectorQuote industry = collected.data().get(0);
        assertThat(industry.sectorCode()).isEqualTo("BK001");
        assertThat(industry.turnover()).isEqualByComparingTo("100000000");
        assertThat(industry.marketCap()).isEqualByComparingTo("500000000");
        MarketDataModels.SectorQuote concept = collected.data().get(1);
        assertThat(concept.turnover()).isNull();
        assertThat(concept.mainNetInflow()).isEqualByComparingTo("3000000");
    }

    @Test
    void shouldKeepIndustryMarketDataWhenSummarySourceFails() {
        TestMarketClient client = new TestMarketClient();
        client.failIndustrySummary = true;

        var collected = new MarketSectorCollector(client).collectSectors();

        assertThat(collected.dataStatus()).isEqualTo(MarketDataStatus.STALE);
        assertThat(collected.data()).extracting(MarketDataModels.SectorQuote::sectorCode)
                .contains("BK001");
        assertThat(collected.message()).contains("同花顺行业汇总");
    }

    private static final class TestMarketClient extends AkToolsMarketClient {

        private boolean failIndustrySummary;

        private TestMarketClient() {
            super(new MarketProperty());
        }

        @Override
        public List<MarketDataModels.SectorQuote> fetchIndustryMarketSectors() {
            return List.of(new MarketDataModels.SectorQuote(
                    "industry", "BK001", "银行", BigDecimal.ONE,
                    null, new BigDecimal("500000000"), null,
                    null, null, null, null));
        }

        @Override
        public List<MarketDataModels.SectorQuote> fetchIndustrySummarySectors() {
            if (failIndustrySummary) {
                throw new IllegalStateException("summary failed");
            }
            return List.of(new MarketDataModels.SectorQuote(
                            "industry", null, " 银行 ", null,
                            new BigDecimal("100000000"), null, null,
                            10, 2, "甲公司", null));
        }

        @Override
        public List<MarketDataModels.SectorQuote> fetchConceptSectors() {
            return List.of(new MarketDataModels.SectorQuote(
                    "concept", "BK100", "人工智能", new BigDecimal("2"),
                    null, new BigDecimal("900000000"), null,
                    null, null, null, null));
        }

        @Override
        public List<MarketDataModels.SectorRanking> fetchSectorRankings() {
            return List.of(new MarketDataModels.SectorRanking(
                    "今日", "concept", "BK100", "人工智能",
                    new BigDecimal("2"), new BigDecimal("3000000")));
        }
    }
}
