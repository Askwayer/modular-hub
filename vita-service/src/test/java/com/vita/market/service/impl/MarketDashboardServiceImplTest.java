package com.vita.market.service.impl;

import com.vita.core.exception.ServiceException;
import com.vita.market.enums.MarketModule;
import com.vita.market.enums.MarketTradeStatus;
import com.vita.market.model.MarketDataModels;
import com.vita.market.model.MarketTradeContext;
import com.vita.market.property.MarketProperty;
import com.vita.market.service.MarketTradeCalendarService;
import com.vita.market.support.MarketSnapshotStore;
import com.vita.market.support.MarketTradeClock;
import com.vita.market.vo.MarketModuleResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketDashboardServiceImplTest {

    @Test
    void shouldNormalizeSectorTypeAndDefaultConceptToMarketCap() {
        MarketDashboardServiceImpl service = service();

        var result = service.heatmap("CONCEPT", null, "ALL", null);

        assertThat(result.getData().availableAreaMetrics()).containsExactly("marketCap");
        assertThat(result.getData().list()).singleElement()
                .satisfies(item -> assertThat(item.sectorType()).isEqualTo("concept"));
    }

    @Test
    void conceptShouldRejectTurnoverAreaMetric() {
        MarketDashboardServiceImpl service = service();

        assertThatThrownBy(() -> service.heatmap("concept", "TURNOVER", "all", null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("概念板块仅支持 marketCap");
    }

    private MarketDashboardServiceImpl service() {
        MarketProperty property = new MarketProperty();
        LocalDate tradeDate = LocalDate.of(2026, 7, 28);
        List<MarketDataModels.SectorQuote> sectors = List.of(
                new MarketDataModels.SectorQuote(
                        "industry", "BK001", "银行", BigDecimal.ONE,
                        new BigDecimal("100"), new BigDecimal("200"), null,
                        10, 2, "甲公司", null),
                new MarketDataModels.SectorQuote(
                        "concept", "BK100", "人工智能", BigDecimal.TEN,
                        null, new BigDecimal("300"), null,
                        20, 1, "乙公司", null));
        return new MarketDashboardServiceImpl(
                new FixedCalendarService(tradeDate),
                new FixedSnapshotStore(property, sectors, tradeDate),
                null, null, null, null, property);
    }

    private static final class FixedCalendarService extends MarketTradeCalendarService {

        private final MarketTradeContext context;

        private FixedCalendarService(LocalDate tradeDate) {
            super(null, null, new MarketTradeClock());
            this.context = new MarketTradeContext(
                    MarketTradeStatus.TRADING, tradeDate, tradeDate);
        }

        @Override
        public MarketTradeContext current() {
            return context;
        }
    }

    private static final class FixedSnapshotStore extends MarketSnapshotStore {

        private final List<MarketDataModels.SectorQuote> sectors;
        private final LocalDate tradeDate;

        private FixedSnapshotStore(MarketProperty property,
                                   List<MarketDataModels.SectorQuote> sectors,
                                   LocalDate tradeDate) {
            super(null, property);
            this.sectors = sectors;
            this.tradeDate = tradeDate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getSnapshot(MarketModule module, LocalDate ignored) {
            return module == MarketModule.SECTORS ? (T) sectors : null;
        }

        @Override
        public LocalDate getRecentTradeDate() {
            return tradeDate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> MarketModuleResult<T> buildResult(
                MarketModule module,
                MarketTradeStatus tradeStatus,
                LocalDate currentDate,
                LocalDate effectiveDate,
                LocalDateTime nextRefreshAt) {
            MarketModuleResult<T> result = new MarketModuleResult<>();
            result.setTradeStatus(tradeStatus);
            result.setTradeDate(currentDate);
            result.setRecentValidDate(effectiveDate);
            result.setNextRefreshAt(nextRefreshAt);
            result.setData(module == MarketModule.SECTORS ? (T) sectors : null);
            return result;
        }
    }
}
