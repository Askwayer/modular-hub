package com.vita.market.support;

import com.vita.market.constant.MarketConstants;
import com.vita.market.enums.MarketDataStatus;
import com.vita.market.model.MarketDataModels;
import com.vita.market.property.MarketProperty;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarketIndexCollectorTest {

    @Test
    void shouldAlwaysReturnNineOrderedSlotsAndFallbackToTencent() {
        TestMarketClient client = new TestMarketClient();
        MarketIndexCollector collector = new MarketIndexCollector(client, Runnable::run);

        var collected = collector.collect(null);

        assertThat(collected.data()).hasSize(9);
        assertThat(collected.data()).extracting(MarketDataModels.IndexQuote::indexCode)
                .containsExactly(
                        "000001", "399001", "399006", "000688", "000680",
                        "000510", "000300", "000016", "399330")
                .doesNotContain("899050");
        MarketDataModels.IndexQuote fallback = collected.data().get(1);
        assertThat(fallback.indexCode()).isEqualTo("399001");
        assertThat(fallback.latestValue()).isEqualByComparingTo("9000");
        assertThat(fallback.dataSource()).isEqualTo("TENCENT");
        assertThat(fallback.dataStatus()).isEqualTo(MarketDataStatus.STALE);
        assertThat(collected.dataStatus()).isEqualTo(MarketDataStatus.STALE);
    }

    @Test
    void shouldReusePreviousSlotWithoutInventingZero() {
        TestMarketClient client = new TestMarketClient();
        client.failCode = "000688";
        MarketIndexCollector collector = new MarketIndexCollector(client, Runnable::run);
        MarketDataModels.IndexQuote previous = new MarketDataModels.IndexQuote(
                "000688", "科创50", new BigDecimal("999"), null, null, null,
                LocalDateTime.of(2026, 7, 27, 15, 0), "SINA",
                MarketDataStatus.FRESH, List.of());

        var collected = collector.collect(List.of(previous));
        MarketDataModels.IndexQuote restored = collected.data().get(3);

        assertThat(restored.latestValue()).isEqualByComparingTo("999");
        assertThat(restored.dataStatus()).isEqualTo(MarketDataStatus.STALE);
        assertThat(restored.dataSource()).contains("CACHE");
    }

    private static final class TestMarketClient extends AkToolsMarketClient {

        private String failCode;

        private TestMarketClient() {
            super(new MarketProperty());
        }

        @Override
        public Map<String, MarketDataModels.IndexSpot> fetchIndexSpots() {
            Map<String, MarketDataModels.IndexSpot> result = new LinkedHashMap<>();
            for (MarketConstants.IndexDefinition definition : MarketConstants.INDEX_DEFINITIONS) {
                if (!"399001".equals(definition.code()) && !definition.code().equals(failCode)) {
                    result.put(definition.code(), new MarketDataModels.IndexSpot(
                            definition.code(), new BigDecimal("3000"),
                            BigDecimal.ONE, new BigDecimal("0.1"), new BigDecimal("100000")));
                }
            }
            return result;
        }

        @Override
        public List<MarketDataModels.CurvePoint> fetchIndexCurve(String indexCode) {
            if ("399001".equals(indexCode)) {
                return List.of();
            }
            if (indexCode.equals(failCode)) {
                throw new IllegalStateException("curve failed");
            }
            return List.of(new MarketDataModels.CurvePoint(
                    LocalDateTime.of(2026, 7, 28, 10, 0), new BigDecimal("3000")));
        }

        @Override
        public List<MarketDataModels.IndexDailyPoint> fetchIndexDaily(String sourceSymbol) {
            String code = sourceSymbol.substring(2);
            if (code.equals(failCode)) {
                throw new IllegalStateException("daily failed");
            }
            BigDecimal close = "399001".equals(code) ? new BigDecimal("9000") : new BigDecimal("3000");
            return List.of(
                    new MarketDataModels.IndexDailyPoint(
                            LocalDate.of(2026, 7, 27), close.subtract(BigDecimal.TEN)),
                    new MarketDataModels.IndexDailyPoint(
                            LocalDate.of(2026, 7, 28), close));
        }
    }
}
