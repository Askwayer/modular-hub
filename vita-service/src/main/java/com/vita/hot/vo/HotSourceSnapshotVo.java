package com.vita.hot.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.vo
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜快照视图对象
 * @Version: 1.0
 */
@Data
public class HotSourceSnapshotVo implements Serializable {

    /**
     * 热榜源代码标识，例如 "weibo", "zhihu" 等
     */
    private String sourceCode;

    /**
     * 热榜源名称，例如 "微博热搜", "知乎热榜" 等
     */
    private String routeCode;

    /**
     * 热榜源名称，例如 "微博热搜", "知乎热榜" 等
     */
    private String sourceName;

    /**
     * 热榜源类型，例如 "social_media", "news_portal" 等
     */
    private String sourceType;

    /**
     * 热榜数据的抓取时间，表示数据的时效性
     */
    private String fetchedAt;

    /**
     * 热搜榜单子项列表，包含每个热搜项的详细信息
     */
    private List<HotItemVo> items;

    /**
     * 热搜榜单子项列表，包含每个热搜项的详细信息
     */
    private List<HotSourceSnapshotVo> sources;

}
