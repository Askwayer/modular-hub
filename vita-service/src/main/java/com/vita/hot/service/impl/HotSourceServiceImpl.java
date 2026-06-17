package com.vita.hot.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.vita.core.RedisCache;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.hot.constant.HotConstants;
import com.vita.hot.property.HotProperty;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.support.HotEngineClient;
import com.vita.hot.vo.HotEngineHealthVo;
import com.vita.hot.vo.HotSourceSnapshotVo;
import com.vita.hot.vo.HotSourceStatusVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    private final Executor hotRefreshTaskExecutor;

    /**
     * 构造函数注入热榜相关依赖
     *
     * @param hotProperty     热榜配置属性对象
     * @param redisCache      Redis缓存操作工具
     * @param redissonClient  Redisson客户端，用于分布式锁等功能
     * @param hotEngineClient 热榜引擎客户端，用于调用Python服务
     */
    public HotSourceServiceImpl(HotProperty hotProperty,
                                RedisCache redisCache,
                                RedissonClient redissonClient,
                                HotEngineClient hotEngineClient,
                                @Qualifier("hotRefreshTaskExecutor") Executor hotRefreshTaskExecutor) {
        this.hotProperty = hotProperty;
        this.redisCache = redisCache;
        this.redissonClient = redissonClient;
        this.hotEngineClient = hotEngineClient;
        this.hotRefreshTaskExecutor = hotRefreshTaskExecutor;
    }


    /**
     * 检查热榜引擎服务的健康状态
     *
     * @return 热榜引擎健康状态信息
     */
    @Override
    public HotEngineHealthVo health() {
        return hotEngineClient.getHealthStatus();
    }

    /**
     * 刷新热榜源注册表信息并写入Redis缓存
     * 从热榜引擎获取所有可用的热榜源列表并更新到缓存中
     *
     * @return 刷新后的热榜源代码列表
     */
    @Override
    public Long refreshSourceRegistry() {
        HotSourceSnapshotVo vo = hotEngineClient.fetchSourceSnapshot("sources");
        Long size = 0L;
        if (CollUtil.isEmpty(vo.getSources())) {
            return size;
        }
        List<String> list = vo.getSources().stream()
                .filter(Objects::nonNull)
                // 提取源代码字段
                .map(HotSourceSnapshotVo::getSourceCode)
                // 过滤 null 和空白字符串
                .filter(CharSequenceUtil::isNotBlank)
                // 去除前后都空格
                .map(CharSequenceUtil::trim)
                // 去重，保留原始顺序
                .distinct()
                .toList();
        size = redisCache.setCacheList(HotConstants.HOT_SOURCE_META_KEY, list);
        redisCache.expire(HotConstants.HOT_SOURCE_META_KEY, hotProperty.getRegistryCacheTtlMinutes());
        return size;
    }

    /**
     * 手动刷新指定热榜源的缓存数据
     * 调用热榜引擎获取最新数据并更新本地缓存
     *
     * @param sourceCode 热榜源代码标识
     * @return 刷新后的热榜快照数据
     */
    @Override
    public HotSourceSnapshotVo refreshSource(String sourceCode) {
        HotSourceSnapshotVo vo = hotEngineClient.fetchSourceSnapshot(sourceCode);
        redisCache.setCacheObject(HotConstants.buildSourceCacheKey(sourceCode), vo, hotProperty.getRegistryCacheTtlMinutes(), TimeUnit.MINUTES);
        return vo;
    }

    /**
     * 批量刷新所有已启用的热榜源缓存。
     * 使用全量刷新锁，避免 admin、scheduler、crontab 同时触发全量刷新。
     *
     * @return 所有热榜源的刷新状态列表
     */
    @Override
    public List<HotSourceStatusVo> refreshAllEnabledSources() {
        RLock lock = redissonClient.getLock(HotConstants.HOT_REFRESH_ALL_LOCK_KEY);
        boolean locked = false;

        try {
            // 尝试获取锁，等待时间为0，锁过期时间为5分钟
            locked = lock.tryLock(0, HotConstants.HOT_REFRESH_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                LOGGER.warn("无法获取全量刷新锁，可能已有其他实例正在执行全量刷新，跳过本次刷新");
                return CollUtil.newArrayList();
            }
            LOGGER.info("成功获取全量刷新锁，开始执行全量刷新");
            List<String> sourceCodes = loadSourceCodes();
            // 源列表为空时，先刷新源列表
            if (CollUtil.isEmpty(sourceCodes)) {
                refreshSourceRegistry();
                sourceCodes = loadSourceCodes();
            }

            if (CollUtil.isEmpty(sourceCodes)) {
                LOGGER.warn("热榜源列表为空，跳过批量刷新");
                return CollUtil.newArrayList();
            }
            // 这里可以考虑并行刷新以提高效率，使用 CompletableFuture 来异步执行刷新操作
            List<CompletableFuture<HotSourceStatusVo>> futures = sourceCodes.stream().map(sourceCode -> CompletableFuture.supplyAsync(
                    () -> refreshSourceAndBuildStatus(sourceCode),
                    hotRefreshTaskExecutor
            )).toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("获取全量刷新热榜源锁被中断", e);
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "获取热榜刷新锁被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 批量刷新所有已启用的热榜源缓存（带降级策略）
     * 当部分源刷新失败时，返回已有的缓存状态而不是抛出异常
     *
     * @return 所有热榜源的刷新状态列表，失败时返回已有缓存
     */
    @Override
    public List<HotSourceStatusVo> refreshAllEnabledSourcesWithFallback() {
        try {
            return refreshAllEnabledSources();
        } catch (Exception e) {
            LOGGER.warn("批量刷新热榜源缓存失败，降级返回已有缓存状态，error={}", e.getMessage(), e);
            return listCacheStatus();
        }
    }

    /**
     * 查询指定热榜源的缓存快照数据
     * 从Redis缓存中读取已缓存的热榜数据
     *
     * @param sourceCode 热榜源代码标识
     * @return 热榜快照数据，缓存不存在或过期时可能返回null
     */
    @Override
    public HotSourceSnapshotVo getCachedSource(String sourceCode) {
        Object cacheObject = redisCache.getCacheObject(HotConstants.buildSourceCacheKey(sourceCode));
        if (ObjectUtil.isNotNull(cacheObject) && cacheObject instanceof HotSourceSnapshotVo snapshot) {
            return snapshot;
        }
        HotSourceSnapshotVo emptySnapshot = new HotSourceSnapshotVo();
        emptySnapshot.setSourceCode(sourceCode);
        emptySnapshot.setItems(CollUtil.newArrayList());
        return emptySnapshot;
    }

    /**
     * 查询所有热榜源的缓存状态信息
     * 包括缓存是否就绪、最后更新时间、数据条数等状态信息
     *
     * @return 所有热榜源的缓存状态列表
     */
    @Override
    public List<HotSourceStatusVo> listCacheStatus() {
        List<String> sourceCodes = loadSourceCodes();
        if (CollUtil.isEmpty(sourceCodes)) {
            LOGGER.warn("热榜源列表为空，无法查询缓存状态");
            return CollUtil.newArrayList();
        }
        return sourceCodes.stream()
                .map(this::buildCachedStatus)
                .toList();
    }

    /**
     * 刷新单个热榜源，并构建刷新状态。
     *
     * @param sourceCode 热榜源代码标识
     * @return 刷新后的热榜源状态对象，包含刷新结果和最新缓存状态
     */
    private HotSourceStatusVo refreshSourceAndBuildStatus(String sourceCode) {
        try {
            HotSourceSnapshotVo snapshot = refreshSource(sourceCode);
            HotSourceStatusVo statusVo = new HotSourceStatusVo();
            statusVo.setSourceCode(sourceCode);
            statusVo.setSourceName(snapshot.getSourceName());
            statusVo.setCached(Boolean.TRUE);
            statusVo.setStale(Boolean.FALSE);
            statusVo.setLastFetchedAt(snapshot.getFetchedAt());
            statusVo.setItemCount(snapshot.getItems() == null ? 0 : snapshot.getItems().size());
            statusVo.setRefreshSuccess(Boolean.TRUE);
            statusVo.setErrorMessage(null);

            redisCache.getExpire(HotConstants.buildSourceCacheKey(sourceCode));
            statusVo.setTtlSeconds(redisCache.getExpire(HotConstants.buildSourceCacheKey(sourceCode)));

            return statusVo;
        } catch (Exception e) {
            LOGGER.warn("刷新热榜源失败，sourceCode={}，error={}", sourceCode, e.getMessage(), e);
            HotSourceStatusVo statusVo = buildCachedStatus(sourceCode);
            statusVo.setRefreshSuccess(Boolean.FALSE);
            statusVo.setErrorMessage(e.getMessage());
            return statusVo;
        }
    }

    /**
     * 从 Redis 中加载热榜源编码列表。
     *
     * @return 热榜源编码列表，如果缓存中没有数据则返回空列表
     */
    private List<String> loadSourceCodes() {
        List<String> sourceCodes = redisCache.getCacheList(HotConstants.HOT_SOURCE_META_KEY);
        if (CollUtil.isEmpty(sourceCodes)) {
            return CollUtil.newArrayList();
        }
        return sourceCodes.stream()
                .filter(CharSequenceUtil::isNotBlank)
                .map(CharSequenceUtil::trim)
                .distinct()
                .toList();
    }

    /**
     * 根据 Redis 中已有缓存构建状态。
     *
     * @param sourceCode 热榜源编码
     *                   - 从 Redis 中读取缓存快照数据和剩余存活时间
     *                   - 判断快照是否过期（ttl <= 0 或快照为 null）
     *                   - 构建 HotSourceStatusVo 对象，设置缓存状态、过期状态、数据条数等信息
     * @return 热榜源状态对象，包含缓存状态、过期状态、数据条数等信息
     */
    private HotSourceStatusVo buildCachedStatus(String sourceCode) {
        String cacheKey = HotConstants.buildSourceCacheKey(sourceCode);
        HotSourceSnapshotVo snapshot = null;
        Object cacheObject = redisCache.getCacheObject(cacheKey);
        if (cacheObject instanceof HotSourceSnapshotVo vo) {
            snapshot = vo;
        }
        Long ttl = redisCache.getExpire(cacheKey);
        HotSourceStatusVo statusVo = new HotSourceStatusVo();
        statusVo.setSourceCode(sourceCode);
        statusVo.setTtlSeconds(ttl);
        statusVo.setRefreshSuccess(null);
        statusVo.setErrorMessage(null);

        if (snapshot == null) {
            statusVo.setCached(Boolean.FALSE);
            statusVo.setStale(Boolean.TRUE);
            statusVo.setItemCount(0);
            return statusVo;
        }

        statusVo.setSourceName(snapshot.getSourceName());
        statusVo.setCached(Boolean.TRUE);
        statusVo.setLastFetchedAt(snapshot.getFetchedAt());
        statusVo.setItemCount(snapshot.getItems() == null ? 0 : snapshot.getItems().size());

        boolean stale = isSnapshotStale(snapshot, ttl);
        statusVo.setStale(stale);

        return statusVo;
    }

    /**
     * 判断缓存快照是否过期（陈旧）。
     * <p>
     * 判定逻辑优先级：
     * 1. 如果 Redis 中该键已过期或不存在（ttl <= 0），视为数据不可用，返回 true。
     * 2. 如果快照对象为空或没有抓取时间，无法判断业务时效性，保守返回 false（避免误报陈旧导致前端展示异常，具体视业务需求而定，此处保持原逻辑）。
     * 3. 基于业务配置的陈旧阈值（staleThresholdMinutes）判断：如果最后抓取时间加上阈值早于当前时间，则视为陈旧。
     *
     * @param snapshot 热榜源快照对象
     * @param ttl      缓存剩余存活时间（秒），null 表示未设置过期时间或键不存在
     * @return true 如果快照被视为陈旧或不可用；否则返回 false
     */
    private boolean isSnapshotStale(HotSourceSnapshotVo snapshot, Long ttl) {
        // 1. 缓存层级失效：TTL 无效表示缓存已过期或被删除
        if (ttl == null || ttl <= 0) {
            return true;
        }

        // 2. 数据层级缺失：无快照或无抓取时间，无法计算业务陈旧度
        // 注意：此处原逻辑返回 false，意味着“不认为它是陈旧的”（可能因为没数据所以不显示，或者由上层处理 null）
        if (snapshot == null || snapshot.getFetchedAt() == null) {
            return false;
        }

        // 3. 业务层级陈旧：检查最后抓取时间是否超过了配置允许的最大陈旧时长
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime fetchedAtTime;
        try {
            fetchedAtTime = LocalDateTime.parse(snapshot.getFetchedAt(), dateTimeFormatter);
        } catch (DateTimeParseException e) {
            return true;
        }
        LocalDateTime staleThresholdTime = fetchedAtTime.plusMinutes(hotProperty.getStaleThresholdMinutes());
        return staleThresholdTime.isBefore(LocalDateTime.now());
    }

}