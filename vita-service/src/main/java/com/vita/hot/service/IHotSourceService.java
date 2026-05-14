package com.vita.hot.service;

import com.vita.hot.vo.HotEngineHealthVo;
import com.vita.hot.vo.HotSourceSnapshotVo;
import com.vita.hot.vo.HotSourceStatusVo;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.service
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜业务服务接口
 * @Version: 1.0
 */
public interface IHotSourceService {

    /**
     * 健康检查 hot-engine。
     */
    HotEngineHealthVo health();

    /**
     * 刷新热榜源注册表信息并写入Redis缓存
     * 从热榜引擎获取所有可用的热榜源列表并更新到缓存中
     *
     * @return 刷新后的热榜源代码列表
     */
    Long refreshSourceRegistry();

    /**
     * 手动刷新单个热榜源缓存。
     */
    HotSourceSnapshotVo refreshSource(String sourceCode);

    /**
     * 刷新全部启用热榜源缓存。
     */
    List<HotSourceStatusVo> refreshAllEnabledSources();

    /**
     * 刷新全部启用热榜源缓存，失败时降级返回已有缓存状态。
     */
    List<HotSourceStatusVo> refreshAllEnabledSourcesWithFallback();

    /**
     * 查询单个热榜源缓存。
     */
    HotSourceSnapshotVo getCachedSource(String sourceCode);

    /**
     * 查询热榜缓存状态。
     */
    List<HotSourceStatusVo> listCacheStatus();
}
