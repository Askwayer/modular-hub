package com.vita.hot.service;

import com.vita.core.RedisCache;
import com.vita.core.exception.ServiceException;
import com.vita.hot.property.HotProperty;
import com.vita.hot.service.impl.HotSourceServiceImpl;
import com.vita.hot.support.HotEngineClient;
import com.vita.hot.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.service
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜业务服务单元测试
 * @Version: 1.0
 */
class HotSourceServiceImplTest {

    private InMemoryRedisCache redisCache;
    private RecordingLock lock;
    private StubHotEngineClient hotEngineClient;
    private HotSourceServiceImpl hotSourceService;

    @BeforeEach
    void setUp() {
        redisCache = new InMemoryRedisCache();
        lock = new RecordingLock();
        hotEngineClient = new StubHotEngineClient();
        RedissonClient redissonClient = buildRedissonClient(lock.asProxy());
        HotProperty hotProperty = new HotProperty();
        hotProperty.setEnabledSources(List.of("weibo", "zhihu"));
        hotProperty.setStaleThresholdMinutes(30L);
        hotSourceService = new HotSourceServiceImpl(hotProperty, redisCache, redissonClient, hotEngineClient);
    }

    @Test
    void getCachedSourceShouldThrowWhenCacheMissing() {
        ServiceException exception = assertThrows(ServiceException.class, () -> hotSourceService.getCachedSource("weibo"));

        assertEquals(503, exception.getCode());
    }

    @Test
    void listCachedSourcesShouldExposeEnabledSourceSummary() {
        HotSourceCacheEntryVo cacheEntry = buildCacheEntry("weibo", Instant.now().toString(), null);
        redisCache.setCacheObject("hot:source:weibo", cacheEntry);

        List<HotSourceSummaryVo> result = hotSourceService.listCachedSources();

        assertEquals(2, result.size());
        assertTrue(result.get(0).getCached());
        assertEquals("weibo", result.get(0).getSourceCode());
        assertFalse(result.get(1).getCached());
    }

    @Test
    void refreshSourceShouldPersistLatestSnapshot() {
        HotSourceSnapshotVo remoteSnapshot = new HotSourceSnapshotVo();
        remoteSnapshot.setSourceCode("weibo");
        remoteSnapshot.setSourceName("微博热搜");
        remoteSnapshot.setFetchedAt(Instant.now().toString());
        HotItemVo itemVo = new HotItemVo();
        itemVo.setRank(1);
        itemVo.setTitle("test");
        itemVo.setUrl("https://example.com");
        remoteSnapshot.setItems(List.of(itemVo));
        hotEngineClient.snapshot = remoteSnapshot;

        HotSourceSnapshotVo result = hotSourceService.refreshSource("weibo");

        assertEquals("weibo", result.getSourceCode());
        assertTrue(redisCache.getCacheObject("hot:source:weibo") instanceof HotSourceCacheEntryVo);
        assertTrue(lock.unlocked);
    }

    @Test
    void listCacheStatusShouldExposeLastError() {
        String staleTime = Instant.now().minusSeconds(3600).toString();
        HotSourceCacheEntryVo cacheEntry = buildCacheEntry("weibo", staleTime, "refresh failed");
        redisCache.setCacheObject("hot:source:weibo", cacheEntry);

        List<HotSourceStatusVo> result = hotSourceService.listCacheStatus();

        assertEquals(2, result.size());
        assertTrue(result.get(0).getStale());
        assertEquals("refresh failed", result.get(0).getLastError());
        assertFalse(result.get(1).getCached());
    }

    private HotSourceCacheEntryVo buildCacheEntry(String sourceCode, String fetchedAt, String lastError) {
        HotSourceCacheEntryVo cacheEntry = new HotSourceCacheEntryVo();
        cacheEntry.setSourceCode(sourceCode);
        cacheEntry.setSourceName("微博热搜");
        cacheEntry.setFetchedAt(fetchedAt);
        cacheEntry.setCachedAt(Instant.now().toString());
        cacheEntry.setLastError(lastError);
        HotItemVo itemVo = new HotItemVo();
        itemVo.setRank(1);
        itemVo.setTitle("demo");
        itemVo.setUrl("https://example.com");
        cacheEntry.setItems(List.of(itemVo));
        return cacheEntry;
    }

    private RedissonClient buildRedissonClient(RLock delegateLock) {
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class[]{RedissonClient.class},
                (proxy, method, args) -> {
                    if ("getLock".equals(method.getName())) {
                        return delegateLock;
                    }
                    if ("shutdown".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestRedissonClient";
                    }
                    throw new UnsupportedOperationException("Unsupported method: " + method.getName());
                }
        );
    }

    private static final class InMemoryRedisCache extends RedisCache {

        private final Map<String, Object> store = new HashMap<>();

        private InMemoryRedisCache() {
            super(null);
        }

        @Override
        public <T> void setCacheObject(String key, T value) {
            store.put(key, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCacheObject(String key) {
            return (T) store.get(key);
        }
    }

    private static final class StubHotEngineClient extends HotEngineClient {

        private HotSourceSnapshotVo snapshot;

        private StubHotEngineClient() {
            super(new HotProperty());
        }

        @Override
        public HotSourceSnapshotVo fetchSourceSnapshot(String sourceCode) {
            return snapshot;
        }
    }

    private static final class RecordingLock {

        private boolean unlocked;

        private RLock asProxy() {
            return (RLock) Proxy.newProxyInstance(
                    RLock.class.getClassLoader(),
                    new Class[]{RLock.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "tryLock":
                                return true;
                            case "isHeldByCurrentThread":
                                return true;
                            case "unlock":
                                unlocked = true;
                                return null;
                            case "toString":
                                return "RecordingLock";
                            default:
                                throw new UnsupportedOperationException("Unsupported method: " + method.getName());
                        }
                    }
            );
        }
    }
}
