package com.vita.market.service;

import com.vita.market.model.MarketTradeContext;
import com.vita.market.property.MarketProperty;
import com.vita.market.support.AkToolsMarketClient;
import com.vita.market.support.MarketSnapshotStore;
import com.vita.market.support.MarketTradeClock;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MarketTradeCalendarServiceTest {

    @Test
    void shouldFetchColdCalendarOnlyOnceForConcurrentReaders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        List<LocalDate> fetchedDates = List.of(today);
        TestMarketClient marketClient = new TestMarketClient(fetchedDates, 100);
        InMemoryMarketSnapshotStore snapshotStore = new InMemoryMarketSnapshotStore();
        MarketTradeCalendarService service = new MarketTradeCalendarService(
                marketClient, snapshotStore, new MarketTradeClock());
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<CompletableFuture<MarketTradeContext>> futures = IntStream.range(0, 8)
                    .mapToObj(index -> CompletableFuture.supplyAsync(service::current, executor))
                    .toList();
            List<MarketTradeContext> contexts = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            assertThat(contexts)
                    .hasSize(8)
                    .allSatisfy(context -> assertThat(context.effectiveTradeDate())
                            .isEqualTo(today));
            assertThat(snapshotStore.getTradeCalendar()).isEqualTo(fetchedDates);
            assertThat(marketClient.fetchCount()).isEqualTo(1);
            assertThat(snapshotStore.saveCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void explicitRefreshShouldReplaceExistingCalendar() {
        List<LocalDate> existingDates = List.of(LocalDate.of(2026, 7, 27));
        List<LocalDate> refreshedDates = List.of(LocalDate.of(2026, 7, 28));
        TestMarketClient marketClient = new TestMarketClient(refreshedDates, 0);
        InMemoryMarketSnapshotStore snapshotStore = new InMemoryMarketSnapshotStore();
        snapshotStore.saveTradeCalendar(existingDates);
        MarketTradeCalendarService service = new MarketTradeCalendarService(
                marketClient, snapshotStore, new MarketTradeClock());

        assertThat(service.refreshCalendar()).isEqualTo(refreshedDates);
        assertThat(snapshotStore.getTradeCalendar()).isEqualTo(refreshedDates);
        assertThat(marketClient.fetchCount()).isEqualTo(1);
        assertThat(snapshotStore.saveCount()).isEqualTo(2);
    }

    private static final class TestMarketClient extends AkToolsMarketClient {

        private final List<LocalDate> fetchedDates;
        private final long delayMs;
        private final AtomicInteger fetchCount = new AtomicInteger();

        private TestMarketClient(List<LocalDate> fetchedDates, long delayMs) {
            super(new MarketProperty());
            this.fetchedDates = fetchedDates;
            this.delayMs = delayMs;
        }

        @Override
        public List<LocalDate> fetchTradeDates() {
            fetchCount.incrementAndGet();
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("测试线程被中断", e);
                }
            }
            return fetchedDates;
        }

        private int fetchCount() {
            return fetchCount.get();
        }
    }

    private static final class InMemoryMarketSnapshotStore extends MarketSnapshotStore {

        private final AtomicReference<List<LocalDate>> cachedDates =
                new AtomicReference<>(Collections.emptyList());
        private final AtomicInteger saveCount = new AtomicInteger();

        private InMemoryMarketSnapshotStore() {
            super(null, new MarketProperty());
        }

        @Override
        public void saveTradeCalendar(List<LocalDate> tradeDates) {
            cachedDates.set(tradeDates);
            saveCount.incrementAndGet();
        }

        @Override
        public List<LocalDate> getTradeCalendar() {
            return cachedDates.get();
        }

        private int saveCount() {
            return saveCount.get();
        }
    }
}
