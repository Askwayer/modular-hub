package com.vita.hot.service.impl;

import com.vita.core.RedisCache;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.hot.constant.HotConstants;
import com.vita.hot.property.HotProperty;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.support.HotEngineClient;
import com.vita.hot.vo.HotSourceCacheEntryVo;
import com.vita.hot.vo.HotSourceSnapshotVo;
import com.vita.hot.vo.HotSourceStatusVo;
import com.vita.hot.vo.HotSourceSummaryVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        List<HotSourceSummaryVo> result = new ArrayList<>();
        for (String sourceCode : getEnabledSourceCodes()) {
            HotSourceCacheEntryVo cacheEntry = getCacheEntry(sourceCode);
            HotSourceSummaryVo summaryVo = new HotSourceSummaryVo();
            summaryVo.setSourceCode(sourceCode);
            summaryVo.setSourceName(resolveSourceName(sourceCode));
            summaryVo.setEnabled(Boolean.TRUE);
            summaryVo.setCached(cacheEntry != null);
            summaryVo.setStale(isStale(cacheEntry == null ? null : cacheEntry.getFetchedAt()));
            summaryVo.setFetchedAt(cacheEntry == null ? null : cacheEntry.getFetchedAt());
            summaryVo.setCachedAt(cacheEntry == null ? null : cacheEntry.getCachedAt());
            summaryVo.setItemCount(cacheEntry == null || CollectionUtils.isEmpty(cacheEntry.getItems())
                    ? 0
                    : cacheEntry.getItems().size());
            result.add(summaryVo);
        }
        return result;
    }

    @Override
    public HotSourceSnapshotVo getCachedSource(String sourceCode) {
        validateEnabledSource(sourceCode);
        HotSourceCacheEntryVo cacheEntry = getCacheEntry(sourceCode);
        if (cacheEntry == null || CollectionUtils.isEmpty(cacheEntry.getItems())) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜缓存未就绪，请稍后重试");
        }
        return toSnapshot(cacheEntry);
    }

    @Override
    public HotSourceSnapshotVo refreshSource(String sourceCode) {
        validateEnabledSource(sourceCode);
        RLock lock = redissonClient.getLock(HotConstants.buildRefreshLockKey(sourceCode));
        boolean locked = false;
        try {
            locked = lock.tryLock(0L, HotConstants.HOT_REFRESH_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException(GlobalErrorCode.LOCKED);
            }
            HotSourceSnapshotVo remoteSnapshot = hotEngineClient.fetchSourceSnapshot(sourceCode);
            HotSourceCacheEntryVo cacheEntry = buildCacheEntry(sourceCode, remoteSnapshot);
            redisCache.setCacheObject(HotConstants.buildSourceCacheKey(sourceCode), cacheEntry);
            return toSnapshot(cacheEntry);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } catch (ServiceException ex) {
            markRefreshFailure(sourceCode, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markRefreshFailure(sourceCode, ex.getMessage());
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "热榜缓存刷新失败");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<HotSourceStatusVo> refreshAllEnabledSources() {
        for (String sourceCode : getEnabledSourceCodes()) {
            try {
                refreshSource(sourceCode);
            } catch (ServiceException ex) {
                LOGGER.warn("刷新热榜缓存失败，sourceCode={}, message={}", sourceCode, ex.getMessage());
            }
        }
        return listCacheStatus();
    }

    @Override
    public List<HotSourceStatusVo> listCacheStatus() {
        List<HotSourceStatusVo> result = new ArrayList<>();
        for (String sourceCode : getEnabledSourceCodes()) {
            HotSourceCacheEntryVo cacheEntry = getCacheEntry(sourceCode);
            HotSourceStatusVo statusVo = new HotSourceStatusVo();
            statusVo.setSourceCode(sourceCode);
            statusVo.setSourceName(resolveSourceName(sourceCode));
            statusVo.setEnabled(Boolean.TRUE);
            statusVo.setCached(cacheEntry != null);
            statusVo.setStale(isStale(cacheEntry == null ? null : cacheEntry.getFetchedAt()));
            statusVo.setFetchedAt(cacheEntry == null ? null : cacheEntry.getFetchedAt());
            statusVo.setCachedAt(cacheEntry == null ? null : cacheEntry.getCachedAt());
            statusVo.setItemCount(cacheEntry == null || CollectionUtils.isEmpty(cacheEntry.getItems())
                    ? 0
                    : cacheEntry.getItems().size());
            statusVo.setLastError(cacheEntry == null ? null : cacheEntry.getLastError());
            result.add(statusVo);
        }
        return result;
    }

    private HotSourceCacheEntryVo buildCacheEntry(String sourceCode, HotSourceSnapshotVo remoteSnapshot) {
        HotSourceCacheEntryVo cacheEntry = new HotSourceCacheEntryVo();
        cacheEntry.setSourceCode(sourceCode);
        cacheEntry.setSourceName(StringUtils.hasText(remoteSnapshot.getSourceName())
                ? remoteSnapshot.getSourceName()
                : resolveSourceName(sourceCode));
        cacheEntry.setFetchedAt(remoteSnapshot.getFetchedAt());
        cacheEntry.setCachedAt(Instant.now().toString());
        cacheEntry.setLastError(null);
        cacheEntry.setItems(remoteSnapshot.getItems() == null ? new ArrayList<>() : remoteSnapshot.getItems());
        return cacheEntry;
    }

    private HotSourceSnapshotVo toSnapshot(HotSourceCacheEntryVo cacheEntry) {
        HotSourceSnapshotVo snapshotVo = new HotSourceSnapshotVo();
        snapshotVo.setSourceCode(cacheEntry.getSourceCode());
        snapshotVo.setSourceName(cacheEntry.getSourceName());
        snapshotVo.setFetchedAt(cacheEntry.getFetchedAt());
        snapshotVo.setCachedAt(cacheEntry.getCachedAt());
        snapshotVo.setStale(isStale(cacheEntry.getFetchedAt()));
        snapshotVo.setItems(cacheEntry.getItems() == null ? new ArrayList<>() : cacheEntry.getItems());
        return snapshotVo;
    }

    private void markRefreshFailure(String sourceCode, String errorMessage) {
        HotSourceCacheEntryVo cacheEntry = getCacheEntry(sourceCode);
        if (cacheEntry == null) {
            return;
        }
        cacheEntry.setLastError(StringUtils.hasText(errorMessage) ? errorMessage : "热榜缓存刷新失败");
        redisCache.setCacheObject(HotConstants.buildSourceCacheKey(sourceCode), cacheEntry);
    }

    private HotSourceCacheEntryVo getCacheEntry(String sourceCode) {
        return redisCache.getCacheObject(HotConstants.buildSourceCacheKey(sourceCode));
    }

    private void validateEnabledSource(String sourceCode) {
        if (!getEnabledSourceCodes().contains(sourceCode)) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "热榜源不存在");
        }
    }

    private List<String> getEnabledSourceCodes() {
        LinkedHashSet<String> sourceCodes = new LinkedHashSet<>();
        List<String> configuredSources = hotProperty.getEnabledSources();
        if (CollectionUtils.isEmpty(configuredSources)) {
            sourceCodes.addAll(HotConstants.DEFAULT_ENABLED_SOURCES);
        } else {
            for (String configuredSource : configuredSources) {
                if (HotConstants.SOURCE_NAME_MAPPING.containsKey(configuredSource)) {
                    sourceCodes.add(configuredSource);
                }
            }
        }
        if (sourceCodes.isEmpty()) {
            sourceCodes.addAll(HotConstants.DEFAULT_ENABLED_SOURCES);
        }
        return new ArrayList<>(sourceCodes);
    }

    private String resolveSourceName(String sourceCode) {
        return HotConstants.SOURCE_NAME_MAPPING.getOrDefault(sourceCode, sourceCode);
    }

    private boolean isStale(String fetchedAt) {
        if (!StringUtils.hasText(fetchedAt)) {
            return true;
        }
        try {
            Instant fetchedInstant = Instant.parse(fetchedAt);
            long thresholdMinutes = hotProperty.getStaleThresholdMinutes() == null
                    ? HotConstants.DEFAULT_STALE_THRESHOLD_MINUTES
                    : hotProperty.getStaleThresholdMinutes();
            return fetchedInstant.isBefore(Instant.now().minus(Duration.ofMinutes(thresholdMinutes)));
        } catch (DateTimeParseException ex) {
            LOGGER.warn("解析热榜时间失败，fetchedAt={}", fetchedAt, ex);
            return true;
        }
    }
}
