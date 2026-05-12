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

    private static final long serialVersionUID = 6025520990857770672L;

    private String sourceCode;

    private String sourceName;

    private Boolean enabled;

    private Boolean cached;

    private Boolean stale;

    private String fetchedAt;

    private String cachedAt;

    private Integer itemCount;

    private String lastError;
}
