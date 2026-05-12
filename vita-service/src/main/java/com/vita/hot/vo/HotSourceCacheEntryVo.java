package com.vita.hot.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.vo
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: Redis 中缓存的热榜快照对象
 * @Version: 1.0
 */
@Data
public class HotSourceCacheEntryVo implements Serializable {

    private static final long serialVersionUID = 5809763501747308137L;

    private String sourceCode;

    private String sourceName;

    private String fetchedAt;

    private String cachedAt;

    private String lastError;

    private List<HotItemVo> items = new ArrayList<>();
}
