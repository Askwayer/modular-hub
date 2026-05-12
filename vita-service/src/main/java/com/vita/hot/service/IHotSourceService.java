package com.vita.hot.service;

import com.vita.hot.vo.HotSourceSnapshotVo;
import com.vita.hot.vo.HotSourceStatusVo;
import com.vita.hot.vo.HotSourceSummaryVo;

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
     * 查询已启用源的缓存摘要。
     *
     * @return 源摘要列表
     */
    List<HotSourceSummaryVo> listCachedSources();

    /**
     * 查询单个源的缓存详情。
     *
     * @param sourceCode 源编码
     * @return 缓存快照
     */
    HotSourceSnapshotVo getCachedSource(String sourceCode);

    /**
     * 手动刷新单个源缓存。
     *
     * @param sourceCode 源编码
     * @return 最新快照
     */
    HotSourceSnapshotVo refreshSource(String sourceCode);

    /**
     * 刷新全部启用源缓存。
     *
     * @return 刷新后的状态列表
     */
    List<HotSourceStatusVo> refreshAllEnabledSources();

    /**
     * 查询全部启用源缓存状态。
     *
     * @return 状态列表
     */
    List<HotSourceStatusVo> listCacheStatus();
}
