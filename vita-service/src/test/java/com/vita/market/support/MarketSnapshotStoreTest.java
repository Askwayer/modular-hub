package com.vita.market.support;

import com.vita.market.enums.MarketDataStatus;
import com.vita.market.enums.MarketModule;
import com.vita.market.property.MarketProperty;
import com.vita.redis.RedisCache;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSnapshotStoreTest {

    @Test
    void shouldSaveStaleSnapshotAndKeepItWhenNextRefreshFails() {
        InMemoryRedisCache redisCache = new InMemoryRedisCache();
        MarketSnapshotStore store = new MarketSnapshotStore(redisCache, new MarketProperty());
        LocalDate tradeDate = LocalDate.of(2026, 7, 28);

        store.saveSnapshot(
                MarketModule.INDICES, tradeDate, List.of("partial"),
                MarketDataStatus.STALE, null, "部分指数降级");
        store.markFailure(MarketModule.INDICES, tradeDate, "下一轮失败");

        assertThat(store.<List<String>>getSnapshot(MarketModule.INDICES, tradeDate))
                .containsExactly("partial");
        assertThat(store.getState(MarketModule.INDICES).getDataStatus())
                .isEqualTo(MarketDataStatus.STALE);
        assertThat(store.getState(MarketModule.INDICES).getMessage())
                .isEqualTo("下一轮失败");
    }

    private static final class InMemoryRedisCache extends RedisCache {

        private final Map<String, Object> values = new HashMap<>();

        private InMemoryRedisCache() {
            super(null);
        }

        @Override
        public <T> void setCacheObject(String key, T value, long timeout, TimeUnit timeUnit) {
            values.put(key, value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCacheObject(String key) {
            return (T) values.get(key);
        }
    }
}
