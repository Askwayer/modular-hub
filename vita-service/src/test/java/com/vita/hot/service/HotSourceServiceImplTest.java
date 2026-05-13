package com.vita.hot.service;

import com.vita.core.RedisCache;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.hot.constant.HotConstants;
import com.vita.hot.property.HotDailyApiProperty;
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
import java.util.concurrent.TimeUnit;

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
    private HotProperty hotProperty;
    private HotSourceServiceImpl hotSourceService;

    @BeforeEach
    void setUp() {
        redisCache = new InMemoryRedisCache();
        lock = new RecordingLock();
        hotEngineClient = new StubHotEngineClient();
        hotEngineClient.registry = buildRegistry();
        RedissonClient redissonClient = buildRedissonClient(lock.asProxy());
        hotProperty = new HotProperty();
        hotProperty.setEnabledSources(List.of());
        hotProperty.setStaleThresholdMinutes(20L);
        hotProperty.setRegistryCacheTtlMinutes(24 * 60L);
        hotProperty.setRefreshParallelism(2);
        hotSourceService = new HotSourceServiceImpl(hotProperty, redisCache, redissonClient, hotEngineClient);
    }

    @Test
    void getCachedSourceShouldThrowWhenCacheMissing() {
        ServiceException exception = assertThrows(ServiceException.class, () -> hotSourceService.getCachedSource("weibo"));

        assertEquals(503, exception.getCode());
        assertEquals(0, hotEngineClient.registryFetchCount);
        assertEquals(0, hotEngineClient.snapshotFetchCount);
    }

    @Test
    void listCachedSourcesShouldReturnEmptyWhenRegistryCacheMissing() {
        List<HotSourceSummaryVo> result = hotSourceService.listCachedSources();

        assertTrue(result.isEmpty());
        assertEquals(0, hotEngineClient.registryFetchCount);
    }

    @Test
    void refreshSourceShouldRejectBlankSourceCode() {
        ServiceException exception = assertThrows(ServiceException.class, () -> hotSourceService.refreshSource(" "));

        assertEquals(GlobalErrorCode.BAD_REQUEST.getCode(), exception.getCode());
    }

    @Test
    void listCachedSourcesShouldExposeDynamicSourceSummary() {
        redisCache.setCacheObject(HotConstants.HOT_SOURCE_REGISTRY_CACHE_KEY, buildRegistry());
        HotSourceCacheEntryVo cacheEntry = buildCacheEntry("weibo", Instant.now().toString(), null);
        redisCache.setCacheObject(HotConstants.buildSourceCacheKey("weibo"), cacheEntry);

        List<HotSourceSummaryVo> result = hotSourceService.listCachedSources();

        assertEquals(2, result.size());
        assertEquals("weibo", result.get(0).getSourceCode());
        assertEquals("weibo", result.get(0).getRouteCode());
        assertEquals("weibo", result.get(0).getIconKey());
        assertEquals("热搜榜", result.get(0).getSourceType());
        assertTrue(result.get(0).getCached());
        assertFalse(result.get(1).getCached());
        assertEquals(0, hotEngineClient.registryFetchCount);
    }

    @Test
    void refreshSourceShouldPersistLatestSnapshot() {
        HotSourceSnapshotVo remoteSnapshot = new HotSourceSnapshotVo();
        remoteSnapshot.setSourceCode("weibo");
        remoteSnapshot.setRouteCode("weibo");
        remoteSnapshot.setSourceName("微博");
        remoteSnapshot.setSourceType("热搜榜");
        remoteSnapshot.setFetchedAt(Instant.now().toString());
        HotItemVo itemVo = new HotItemVo();
        itemVo.setRank(1);
        itemVo.setTitle("test");
        itemVo.setUrl("https://example.com");
        remoteSnapshot.setItems(List.of(itemVo));
        hotEngineClient.snapshot = remoteSnapshot;

        HotSourceSnapshotVo result = hotSourceService.refreshSource("weibo");

        assertEquals("weibo", result.getSourceCode());
        assertEquals("weibo", result.getRouteCode());
        assertTrue(redisCache.getCacheObject(HotConstants.buildSourceCacheKey("weibo")) instanceof HotSourceCacheEntryVo);
        assertEquals(HotConstants.HOT_CACHE_TTL_MINUTES,
                redisCache.getTimeout(HotConstants.buildSourceCacheKey("weibo"), TimeUnit.MINUTES));
        assertEquals(hotProperty.getRegistryCacheTtlMinutes(),
                redisCache.getTimeout(HotConstants.HOT_SOURCE_REGISTRY_CACHE_KEY, TimeUnit.MINUTES));
        assertEquals(1, hotEngineClient.registryFetchCount);
        assertEquals(1, hotEngineClient.snapshotFetchCount);
        assertTrue(lock.unlocked);
    }

    @Test
    void refreshSourceShouldNotOverwriteCacheWhenLockBusy() {
        HotSourceCacheEntryVo cacheEntry = buildCacheEntry("weibo", Instant.now().toString(), "keep");
        redisCache.setCacheObject(HotConstants.buildSourceCacheKey("weibo"), cacheEntry);
        lock.tryLockResult = false;

        ServiceException exception = assertThrows(ServiceException.class, () -> hotSourceService.refreshSource("weibo"));

        assertEquals(GlobalErrorCode.LOCKED.getCode(), exception.getCode());
        assertSame(cacheEntry, redisCache.getCacheObject(HotConstants.buildSourceCacheKey("weibo")));
        assertEquals("keep", cacheEntry.getLastError());
        assertFalse(lock.unlocked);
    }

    @Test
    void refreshSourceShouldRecordLastErrorWhenFetchFailed() {
        hotEngineClient.snapshotException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "remote down");

        ServiceException exception = assertThrows(ServiceException.class, () -> hotSourceService.refreshSource("weibo"));

        assertEquals(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        HotSourceCacheEntryVo cacheEntry = redisCache.getCacheObject(HotConstants.buildSourceCacheKey("weibo"));
        assertEquals("remote down", cacheEntry.getLastError());
        assertFalse(Boolean.TRUE.equals(cacheEntry.getSnapshotReady()));
    }

    @Test
    void refreshSourceShouldFallbackToCachedRegistryWhenRemoteRegistryFails() {
        hotProperty.setEnabledSources(List.of("weibo"));
        redisCache.setCacheObject(HotConstants.HOT_SOURCE_REGISTRY_CACHE_KEY, buildRegistry());
        hotEngineClient.registryException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "registry down");
        hotEngineClient.snapshot = buildRemoteSnapshot("weibo");

        HotSourceSnapshotVo result = hotSourceService.refreshSource("weibo");

        assertEquals("weibo", result.getSourceCode());
        assertEquals("微博", result.getSourceName());
    }

    @Test
    void listCacheStatusShouldExposeLastErrorForKnownSource() {
        redisCache.setCacheObject(HotConstants.HOT_SOURCE_REGISTRY_CACHE_KEY, buildRegistry());
        String staleTime = Instant.now().minusSeconds(25 * 60L).toString();
        HotSourceCacheEntryVo cacheEntry = buildCacheEntry("weibo", staleTime, "refresh failed");
        redisCache.setCacheObject(HotConstants.buildSourceCacheKey("weibo"), cacheEntry);

        List<HotSourceStatusVo> result = hotSourceService.listCacheStatus();

        assertEquals(2, result.size());
        assertTrue(result.get(0).getStale());
        assertEquals("refresh failed", result.get(0).getLastError());
        assertFalse(result.get(1).getCached());
        assertEquals(0, hotEngineClient.registryFetchCount);
    }

    @Test
    void refreshAllEnabledSourcesForSchedulerShouldFallbackToCachedRegistryWhenRemoteRegistryFails() {
        redisCache.setCacheObject(HotConstants.HOT_SOURCE_REGISTRY_CACHE_KEY, buildRegistry());
        hotEngineClient.registryException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "registry down");
        hotEngineClient.snapshot = buildRemoteSnapshot("weibo");

        HotRefreshExecutionResult result = hotSourceService.refreshAllEnabledSourcesForScheduler();

        assertFalse(result.skipped());
        assertFalse(result.registryFetchedFromRemote());
        assertTrue(result.registryFallbackToCache());
        assertEquals(2, result.statusList().size());
        assertEquals(1, hotEngineClient.registryFetchCount);
        assertEquals(2, hotEngineClient.snapshotFetchCount);
    }

    @Test
    void refreshAllEnabledSourcesForSchedulerShouldSkipWhenRegistryUnavailableWithoutCache() {
        hotEngineClient.registryException = new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "registry down");

        HotRefreshExecutionResult result = hotSourceService.refreshAllEnabledSourcesForScheduler();

        assertTrue(result.skipped());
        assertFalse(result.registryFetchedFromRemote());
        assertFalse(result.registryFallbackToCache());
        assertTrue(result.statusList().isEmpty());
        assertEquals(1, hotEngineClient.registryFetchCount);
        assertEquals(0, hotEngineClient.snapshotFetchCount);
    }

    private HotSourceRegistryVo buildRegistry() {
        HotSourceRegistryVo registryVo = new HotSourceRegistryVo();
        registryVo.setSources(List.of(
                buildSourceDefinition("weibo", "weibo", "微博", "热搜榜", true),
                buildSourceDefinition("zhihu", "zhihu", "知乎", "热榜", false)
        ));
        return registryVo;
    }

    private HotSourceDefinitionVo buildSourceDefinition(String sourceCode,
                                                        String routeCode,
                                                        String sourceName,
                                                        String sourceType,
                                                        boolean defaultVisible) {
        HotSourceDefinitionVo definitionVo = new HotSourceDefinitionVo();
        definitionVo.setSourceCode(sourceCode);
        definitionVo.setRouteCode(routeCode);
        definitionVo.setIconKey(routeCode);
        definitionVo.setSourceName(sourceName);
        definitionVo.setSourceType(sourceType);
        definitionVo.setDescription("");
        definitionVo.setEnabled(Boolean.TRUE);
        definitionVo.setDefaultVisible(defaultVisible);
        return definitionVo;
    }

    private HotSourceCacheEntryVo buildCacheEntry(String sourceCode, String fetchedAt, String lastError) {
        HotSourceCacheEntryVo cacheEntry = new HotSourceCacheEntryVo();
        cacheEntry.setSourceCode(sourceCode);
        cacheEntry.setRouteCode(sourceCode);
        cacheEntry.setSourceName("微博");
        cacheEntry.setSourceType("热搜榜");
        cacheEntry.setFetchedAt(fetchedAt);
        cacheEntry.setCachedAt(Instant.now().toString());
        cacheEntry.setLastError(lastError);
        cacheEntry.setSnapshotReady(Boolean.TRUE);
        HotItemVo itemVo = new HotItemVo();
        itemVo.setRank(1);
        itemVo.setTitle("demo");
        itemVo.setUrl("https://example.com");
        cacheEntry.setItems(List.of(itemVo));
        return cacheEntry;
    }

    private HotSourceSnapshotVo buildRemoteSnapshot(String sourceCode) {
        HotSourceSnapshotVo remoteSnapshot = new HotSourceSnapshotVo();
        remoteSnapshot.setSourceCode(sourceCode);
        remoteSnapshot.setRouteCode(sourceCode);
        remoteSnapshot.setSourceName("微博");
        remoteSnapshot.setSourceType("热搜榜");
        remoteSnapshot.setFetchedAt(Instant.now().toString());
        HotItemVo itemVo = new HotItemVo();
        itemVo.setRank(1);
        itemVo.setTitle("demo");
        itemVo.setUrl("https://example.com");
        remoteSnapshot.setItems(List.of(itemVo));
        return remoteSnapshot;
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
        private final Map<String, Long> timeoutSeconds = new HashMap<>();

        private InMemoryRedisCache() {
            super(null);
        }

        @Override
        public <T> void setCacheObject(String key, T value) {
            store.put(key, value);
            timeoutSeconds.remove(key);
        }

        @Override
        public <T> void setCacheObject(String key, T value, long timeout, TimeUnit timeUnit) {
            store.put(key, value);
            timeoutSeconds.put(key, TimeUnit.SECONDS.convert(timeout, timeUnit));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCacheObject(String key) {
            return (T) store.get(key);
        }

        private long getTimeout(String key, TimeUnit timeUnit) {
            Long seconds = timeoutSeconds.get(key);
            if (seconds == null) {
                return -1L;
            }
            return timeUnit.convert(seconds, TimeUnit.SECONDS);
        }
    }

    private static final class StubHotEngineClient extends HotEngineClient {

        private HotSourceSnapshotVo snapshot;
        private HotSourceRegistryVo registry;
        private RuntimeException snapshotException;
        private RuntimeException registryException;
        private int snapshotFetchCount;
        private int registryFetchCount;

        private StubHotEngineClient() {
            super(new HotDailyApiProperty());
        }

        @Override
        public HotSourceSnapshotVo fetchSourceSnapshot(String sourceCode) {
            snapshotFetchCount++;
            if (snapshotException != null) {
                throw snapshotException;
            }
            return snapshot;
        }

        @Override
        public HotSourceRegistryVo fetchSourceRegistry() {
            registryFetchCount++;
            if (registryException != null) {
                throw registryException;
            }
            return registry;
        }
    }

    private static final class RecordingLock {

        private boolean unlocked;
        private boolean tryLockResult = true;

        private RLock asProxy() {
            return (RLock) Proxy.newProxyInstance(
                    RLock.class.getClassLoader(),
                    new Class[]{RLock.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "tryLock":
                                return tryLockResult;
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
