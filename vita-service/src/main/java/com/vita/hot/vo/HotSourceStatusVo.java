package com.vita.hot.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.vo
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜缓存状态对象
 * @Version: 1.0
 */
@Data
public class HotSourceStatusVo implements Serializable {

    /**
     * 热榜源代码标识，例如 "weibo", "zhihu" 等
     */
    private String sourceCode;

    /**
     * 热榜源名称，例如 "微博热搜", "知乎热榜" 等
     */
    private String sourceName;

    /**
     * 缓存是否就绪标志，true 表示该热榜源已有缓存数据
     */
    private Boolean cached;

    /**
     * 缓存是否过期标志，true 表示缓存已过期需要刷新
     */
    private Boolean stale;

    /**
     * 缓存剩余存活时间（秒），null 表示未设置过期时间
     */
    private Long ttlSeconds;

    /**
     * 最后一次成功抓取数据的时间
     */
    private String lastFetchedAt;

    /**
     * 热榜数据条目数量
     */
    private Integer itemCount;

    /**
     * 最近一次刷新操作是否成功，null 表示未执行过刷新
     */
    private Boolean refreshSuccess;

    /**
     * 刷新失败时的错误信息，成功时为 null
     */
    private String errorMessage;
}
