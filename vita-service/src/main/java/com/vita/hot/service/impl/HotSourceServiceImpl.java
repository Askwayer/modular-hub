package com.vita.hot.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.vita.core.RedisCache;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.hot.constant.HotConstants;
import com.vita.hot.property.HotProperty;
import com.vita.hot.service.HotRefreshExecutionResult;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.support.HotEngineClient;
import com.vita.hot.vo.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.service.impl
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜业务服务实现
 * @Version: 1.0
 */
@Service
public class HotSourceServiceImpl implements IHotSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotSourceServiceImpl.class);

    private final HotProperty hotProperty;
    private final RedisCache redisCache;
    private final RedissonClient redissonClient;
    private final HotEngineClient hotEngineClient;

    public HotSourceServiceImpl(HotProperty hotProperty,
                                RedisCache redisCache,
                                RedissonClient redissonClient,
                                HotEngineClient hotEngineClient) {
        this.hotProperty = hotProperty;
        this.redisCache = redisCache;
        this.redissonClient = redissonClient;
        this.hotEngineClient = hotEngineClient;
    }

    @Override
    public List<HotSourceSummaryVo> listCachedSources() {
        return buildViewList(getCachedSourceDefinitions(), this::buildSummary);
    }

    @Override
    public HotSourceSnapshotVo getCachedSource(String sourceCode) {
        HotSourceCacheEntryVo cacheEntry = getCacheEntry(normalizeSourceCode(sourceCode));
        if (!isSnapshotReady(cacheEntry)) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜缓存未就绪，请稍后重试");
        }
        return buildSnapshot(cacheEntry);
    }

    @Override
    public HotSourceSnapshotVo refreshSource(String sourceCode) {
        HotSourceDefinitionVo sourceDefinition = resolveSourceDefinition(sourceCode, true);
        return refreshSourceInternal(sourceDefinition);
    }

    @Override
    public List<HotSourceStatusVo> refreshAllEnabledSources() {
        SourceDefinitionsLoadResult loadResult = loadSourceDefinitionsForRefresh(true);
        if (!loadResult.available()) {
            throw loadResult.exception();
        }
        return refreshLoadedSources(loadResult.sourceDefinitions());
    }

    @Override
    public HotRefreshExecutionResult refreshAllEnabledSourcesForScheduler() {
        SourceDefinitionsLoadResult loadResult = loadSourceDefinitionsForRefresh(true);
        if (!loadResult.available()) {
            return new HotRefreshExecutionResult(CollUtil.newArrayList(), false, false, true);
        }
        List<HotSourceStatusVo> statusList = refreshLoadedSources(loadResult.sourceDefinitions());
        return new HotRefreshExecutionResult(
                statusList,
                loadResult.loadedFromRemote(),
                loadResult.loadedFromCache(),
                false
        );
    }

    @Override
    public List<HotSourceStatusVo> listCacheStatus() {
        return buildViewList(getCachedSourceDefinitions(), this::buildStatus);
    }

    private List<HotSourceStatusVo> refreshLoadedSources(List<HotSourceDefinitionVo> sourceDefinitions) {
        if (CollUtil.isEmpty(sourceDefinitions)) {
            return CollUtil.newArrayList();
        }

        int parallelism = resolveRefreshParallelism(sourceDefinitions.size());
        if (parallelism <= 1 || sourceDefinitions.size() <= 1) {
            for (HotSourceDefinitionVo sourceDefinition : sourceDefinitions) {
                refreshSilently(sourceDefinition);
            }
            return buildViewList(sourceDefinitions, this::buildStatus);
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            List<Future<?>> futures = new ArrayList<>(sourceDefinitions.size());
            for (HotSourceDefinitionVo sourceDefinition : sourceDefinitions) {
                futures.add(executor.submit(() -> refreshSilently(sourceDefinition)));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception ex) {
                    LOGGER.warn("等待热榜缓存刷新任务完成失败", ex);
                }
            }
        } finally {
            executor.shutdown();
        }
        return buildViewList(sourceDefinitions, this::buildStatus);
    }

    private void refreshSilently(HotSourceDefinitionVo sourceDefinition) {
        try {
            refreshSourceInternal(sourceDefinition);
        } catch (ServiceException ex) {
            LOGGER.warn("刷新热榜缓存失败，sourceCode={}, message={}", sourceDefinition.getSourceCode(), ex.getMessage());
        }
    }

    /**
     * 在分布式锁保护下刷新单个热榜源，并把最新快照写回 Redis。
     */
    private HotSourceSnapshotVo refreshSourceInternal(HotSourceDefinitionVo sourceDefinition) {
        RLock lock = redissonClient.getLock(HotConstants.buildRefreshLockKey(sourceDefinition.getSourceCode()));
        boolean locked = false;
        try {
            locked = lock.tryLock(0L, HotConstants.HOT_REFRESH_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException(GlobalErrorCode.LOCKED);
            }
            HotSourceSnapshotVo remoteSnapshot = hotEngineClient.fetchSourceSnapshot(sourceDefinition.getSourceCode());
            HotSourceCacheEntryVo cacheEntry = buildCacheEntry(sourceDefinition, remoteSnapshot);
            setCacheEntry(cacheEntry);
            return buildSnapshot(cacheEntry);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } catch (ServiceException ex) {
            if (ObjectUtil.notEqual(ex.getCode(), GlobalErrorCode.LOCKED.getCode())) {
                markRefreshFailure(sourceDefinition, ex.getMessage());
            }
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("刷新热榜缓存发生异常，sourceCode={}", sourceDefinition.getSourceCode(), ex);
            markRefreshFailure(sourceDefinition, ex.getMessage());
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜缓存刷新失败");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private HotSourceCacheEntryVo buildCacheEntry(HotSourceDefinitionVo sourceDefinition, HotSourceSnapshotVo remoteSnapshot) {
        HotSourceCacheEntryVo cacheEntry = new HotSourceCacheEntryVo();
        cacheEntry.setSourceCode(sourceDefinition.getSourceCode());
        cacheEntry.setRouteCode(pickText(remoteSnapshot.getRouteCode(), sourceDefinition.getRouteCode()));
        cacheEntry.setSourceName(pickText(remoteSnapshot.getSourceName(), sourceDefinition.getSourceName()));
        cacheEntry.setSourceType(pickText(remoteSnapshot.getSourceType(), sourceDefinition.getSourceType()));
        cacheEntry.setFetchedAt(remoteSnapshot.getFetchedAt());
        cacheEntry.setCachedAt(Instant.now().toString());
        cacheEntry.setLastError(null);
        cacheEntry.setSnapshotReady(Boolean.TRUE);
        cacheEntry.setItems(copyItems(remoteSnapshot.getItems()));
        return cacheEntry;
    }

    private HotSourceSnapshotVo buildSnapshot(HotSourceCacheEntryVo cacheEntry) {
        HotSourceSnapshotVo snapshotVo = new HotSourceSnapshotVo();
        snapshotVo.setSourceCode(cacheEntry.getSourceCode());
        snapshotVo.setRouteCode(cacheEntry.getRouteCode());
        snapshotVo.setSourceName(cacheEntry.getSourceName());
        snapshotVo.setSourceType(cacheEntry.getSourceType());
        snapshotVo.setFetchedAt(cacheEntry.getFetchedAt());
        snapshotVo.setCachedAt(cacheEntry.getCachedAt());
        snapshotVo.setStale(isStale(cacheEntry.getFetchedAt()));
        snapshotVo.setItems(copyItems(cacheEntry.getItems()));
        return snapshotVo;
    }

    private HotSourceSummaryVo buildSummary(HotSourceDefinitionVo sourceDefinition, HotSourceCacheEntryVo cacheEntry) {
        HotSourceSummaryVo summaryVo = new HotSourceSummaryVo();
        fillDefinition(summaryVo, sourceDefinition);
        summaryVo.setCached(isSnapshotReady(cacheEntry));
        summaryVo.setStale(isStale(getFetchedAt(cacheEntry)));
        summaryVo.setFetchedAt(getFetchedAt(cacheEntry));
        summaryVo.setCachedAt(getCachedAt(cacheEntry));
        summaryVo.setItemCount(getItemCount(cacheEntry));
        return summaryVo;
    }

    private HotSourceStatusVo buildStatus(HotSourceDefinitionVo sourceDefinition, HotSourceCacheEntryVo cacheEntry) {
        HotSourceStatusVo statusVo = new HotSourceStatusVo();
        fillDefinition(statusVo, sourceDefinition);
        statusVo.setCached(isSnapshotReady(cacheEntry));
        statusVo.setStale(isStale(getFetchedAt(cacheEntry)));
        statusVo.setFetchedAt(getFetchedAt(cacheEntry));
        statusVo.setCachedAt(getCachedAt(cacheEntry));
        statusVo.setItemCount(getItemCount(cacheEntry));
        statusVo.setLastError(cacheEntry == null ? null : cacheEntry.getLastError());
        return statusVo;
    }

    private void fillDefinition(HotSourceSummaryVo summaryVo, HotSourceDefinitionVo sourceDefinition) {
        BeanUtil.copyProperties(sourceDefinition, summaryVo);
        summaryVo.setEnabled(BooleanUtil.isTrue(sourceDefinition.getEnabled()));
        summaryVo.setDefaultVisible(BooleanUtil.isTrue(sourceDefinition.getDefaultVisible()));
    }

    private void fillDefinition(HotSourceStatusVo statusVo, HotSourceDefinitionVo sourceDefinition) {
        BeanUtil.copyProperties(sourceDefinition, statusVo);
        statusVo.setEnabled(BooleanUtil.isTrue(sourceDefinition.getEnabled()));
        statusVo.setDefaultVisible(BooleanUtil.isTrue(sourceDefinition.getDefaultVisible()));
    }

    /**
     * 当远端抓取失败时保留最近一次快照，并记录最新失败原因。
     */
    private void markRefreshFailure(HotSourceDefinitionVo sourceDefinition, String errorMessage) {
        HotSourceCacheEntryVo cacheEntry = ObjectUtil.defaultIfNull(
                getCacheEntry(sourceDefinition.getSourceCode()),
                createEmptyCacheEntry(sourceDefinition)
        );
        cacheEntry.setCachedAt(Instant.now().toString());
        cacheEntry.setLastError(CharSequenceUtil.blankToDefault(errorMessage, "热榜缓存刷新失败"));
        setCacheEntry(cacheEntry);
    }

    private HotSourceDefinitionVo resolveSourceDefinition(String sourceCode, boolean forceSync) {
        String normalizedSourceCode = normalizeSourceCode(sourceCode);
        SourceDefinitionsLoadResult loadResult = loadSourceDefinitionsForRefresh(forceSync);
        if (!loadResult.available()) {
            throw loadResult.exception();
        }
        List<HotSourceDefinitionVo> sourceDefinitions = loadResult.sourceDefinitions();
        for (HotSourceDefinitionVo sourceDefinition : sourceDefinitions) {
            if (CharSequenceUtil.equals(normalizedSourceCode, sourceDefinition.getSourceCode())) {
                return sourceDefinition;
            }
        }
        if (!forceSync) {
            return resolveSourceDefinition(normalizedSourceCode, true);
        }
        throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "热榜源不存在");
    }

    /**
     * 刷新链路优先回源同步热榜源目录，并在失败时回退到最近一次缓存目录。
     */
    private SourceDefinitionsLoadResult loadSourceDefinitionsForRefresh(boolean forceSync) {
        if (forceSync) {
            try {
                return SourceDefinitionsLoadResult.loadedFromRemote(syncSourceDefinitions());
            } catch (ServiceException ex) {
                List<HotSourceDefinitionVo> cachedSourceDefinitions = getCachedSourceDefinitions();
                if (CollUtil.isNotEmpty(cachedSourceDefinitions)) {
                    return SourceDefinitionsLoadResult.loadedFromCache(cachedSourceDefinitions, ex);
                }
                return SourceDefinitionsLoadResult.unavailable(ex);
            }
        }
        return SourceDefinitionsLoadResult.loadedFromCache(getCachedSourceDefinitions(), null);
    }

    /**
     * 从远端同步热榜源目录，并把原始目录快照回写到 Redis。
     */
    private List<HotSourceDefinitionVo> syncSourceDefinitions() {
        HotSourceRegistryVo registryVo = hotEngineClient.fetchSourceRegistry();
        List<HotSourceDefinitionVo> filteredSources = filterEnabledSources(registryVo.getSources());
        if (CollUtil.isEmpty(filteredSources)) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜源目录为空");
        }
        HotSourceRegistryVo cacheRegistry = new HotSourceRegistryVo();
        cacheRegistry.setSyncedAt(Instant.now().toString());
        cacheRegistry.setSources(CollUtil.isEmpty(registryVo.getSources())
                ? CollUtil.newArrayList()
                : CollUtil.newArrayList(registryVo.getSources()));
        setSourceRegistry(cacheRegistry);
        return filteredSources;
    }

    private List<HotSourceDefinitionVo> filterEnabledSources(List<HotSourceDefinitionVo> remoteSources) {
        if (CollUtil.isEmpty(remoteSources)) {
            return CollUtil.newArrayList();
        }
        Set<String> allowList = getConfiguredAllowList();
        List<HotSourceDefinitionVo> filtered = new ArrayList<>();
        for (HotSourceDefinitionVo remoteSource : remoteSources) {
            if (remoteSource == null || !BooleanUtil.isTrue(remoteSource.getEnabled())) {
                continue;
            }
            if (CollUtil.isNotEmpty(allowList) && !allowList.contains(remoteSource.getSourceCode())) {
                continue;
            }
            filtered.add(remoteSource);
        }
        return filtered;
    }

    private Set<String> getConfiguredAllowList() {
        LinkedHashSet<String> allowList = new LinkedHashSet<>();
        if (CollUtil.isEmpty(hotProperty.getEnabledSources())) {
            return allowList;
        }
        for (String sourceCode : hotProperty.getEnabledSources()) {
            String normalizedSourceCode = CharSequenceUtil.trim(sourceCode);
            if (CharSequenceUtil.isNotBlank(normalizedSourceCode)) {
                allowList.add(normalizedSourceCode);
            }
        }
        return allowList;
    }

    private List<HotSourceDefinitionVo> getCachedSourceDefinitions() {
        HotSourceRegistryVo registryVo = redisCache.getCacheObject(HotConstants.HOT_SOURCE_REGISTRY_CACHE_KEY);
        if (registryVo == null || CollUtil.isEmpty(registryVo.getSources())) {
            return CollUtil.newArrayList();
        }
        return filterEnabledSources(registryVo.getSources());
    }

    private HotSourceCacheEntryVo getCacheEntry(String sourceCode) {
        return redisCache.getCacheObject(HotConstants.buildSourceCacheKey(sourceCode));
    }

    private void setCacheEntry(HotSourceCacheEntryVo cacheEntry) {
        redisCache.setCacheObject(
                HotConstants.buildSourceCacheKey(cacheEntry.getSourceCode()),
                cacheEntry,
                HotConstants.HOT_CACHE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private void setSourceRegistry(HotSourceRegistryVo cacheRegistry) {
        redisCache.setCacheObject(
                HotConstants.HOT_SOURCE_REGISTRY_CACHE_KEY,
                cacheRegistry,
                resolveRegistryCacheTtlMinutes(),
                TimeUnit.MINUTES
        );
    }

    private long resolveRegistryCacheTtlMinutes() {
        Long configuredTtl = hotProperty.getRegistryCacheTtlMinutes();
        if (configuredTtl == null || configuredTtl <= 0) {
            return HotConstants.DEFAULT_REGISTRY_CACHE_TTL_MINUTES;
        }
        return configuredTtl;
    }

    private boolean isSnapshotReady(HotSourceCacheEntryVo cacheEntry) {
        return cacheEntry != null && BooleanUtil.isTrue(cacheEntry.getSnapshotReady());
    }

    private int resolveRefreshParallelism(int sourceCount) {
        Integer configuredParallelism = hotProperty.getRefreshParallelism();
        int parallelism = configuredParallelism == null || configuredParallelism <= 0
                ? HotConstants.DEFAULT_REFRESH_PARALLELISM
                : configuredParallelism;
        return Math.max(1, Math.min(parallelism, sourceCount));
    }

    /**
     * 按配置的陈旧阈值判断当前快照是否已经过期。
     */
    private boolean isStale(String fetchedAt) {
        if (CharSequenceUtil.isBlank(fetchedAt)) {
            return true;
        }
        try {
            Instant fetchedInstant = Instant.parse(fetchedAt);
            Long configuredThreshold = hotProperty.getStaleThresholdMinutes();
            long thresholdMinutes = configuredThreshold == null || configuredThreshold <= 0
                    ? HotConstants.DEFAULT_STALE_THRESHOLD_MINUTES
                    : configuredThreshold;
            return fetchedInstant.isBefore(Instant.now().minus(Duration.ofMinutes(thresholdMinutes)));
        } catch (DateTimeParseException ex) {
            LOGGER.warn("解析热榜时间失败，fetchedAt={}", fetchedAt, ex);
            return true;
        }
    }

    private <T> List<T> buildViewList(List<HotSourceDefinitionVo> sourceDefinitions,
                                      BiFunction<HotSourceDefinitionVo, HotSourceCacheEntryVo, T> assembler) {
        if (CollUtil.isEmpty(sourceDefinitions)) {
            return CollUtil.newArrayList();
        }
        List<T> result = new ArrayList<>(sourceDefinitions.size());
        for (HotSourceDefinitionVo sourceDefinition : sourceDefinitions) {
            result.add(assembler.apply(sourceDefinition, getCacheEntry(sourceDefinition.getSourceCode())));
        }
        return result;
    }

    private HotSourceCacheEntryVo createEmptyCacheEntry(HotSourceDefinitionVo sourceDefinition) {
        HotSourceCacheEntryVo cacheEntry = new HotSourceCacheEntryVo();
        cacheEntry.setSourceCode(sourceDefinition.getSourceCode());
        cacheEntry.setRouteCode(sourceDefinition.getRouteCode());
        cacheEntry.setSourceName(sourceDefinition.getSourceName());
        cacheEntry.setSourceType(sourceDefinition.getSourceType());
        cacheEntry.setItems(CollUtil.newArrayList());
        cacheEntry.setSnapshotReady(Boolean.FALSE);
        return cacheEntry;
    }

    private String normalizeSourceCode(String sourceCode) {
        String normalizedSourceCode = CharSequenceUtil.trim(sourceCode);
        if (CharSequenceUtil.isBlank(normalizedSourceCode)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return normalizedSourceCode;
    }

    private String pickText(String preferred, String fallback) {
        return CharSequenceUtil.blankToDefault(CharSequenceUtil.trim(preferred), fallback);
    }

    private List<HotItemVo> copyItems(List<HotItemVo> items) {
        return CollUtil.isEmpty(items) ? CollUtil.newArrayList() : CollUtil.newArrayList(items);
    }

    private String getFetchedAt(HotSourceCacheEntryVo cacheEntry) {
        return cacheEntry == null ? null : cacheEntry.getFetchedAt();
    }

    private String getCachedAt(HotSourceCacheEntryVo cacheEntry) {
        return cacheEntry == null ? null : cacheEntry.getCachedAt();
    }

    private Integer getItemCount(HotSourceCacheEntryVo cacheEntry) {
        return cacheEntry == null ? 0 : copyItems(cacheEntry.getItems()).size();
    }

    private record SourceDefinitionsLoadResult(
            List<HotSourceDefinitionVo> sourceDefinitions,
            boolean loadedFromRemote,
            boolean loadedFromCache,
            ServiceException exception
    ) {

        private static SourceDefinitionsLoadResult loadedFromRemote(List<HotSourceDefinitionVo> sourceDefinitions) {
            return new SourceDefinitionsLoadResult(sourceDefinitions, true, false, null);
        }

        private static SourceDefinitionsLoadResult loadedFromCache(List<HotSourceDefinitionVo> sourceDefinitions,
                                                                  ServiceException exception) {
            return new SourceDefinitionsLoadResult(sourceDefinitions, false, true, exception);
        }

        private static SourceDefinitionsLoadResult unavailable(ServiceException exception) {
            return new SourceDefinitionsLoadResult(CollUtil.newArrayList(), false, false, exception);
        }

        private boolean available() {
            return exception == null || CollUtil.isNotEmpty(sourceDefinitions);
        }
    }
}
