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
 * @Description: 热榜快照视图对象
 * @Version: 1.0
 */
@Data
public class HotSourceSnapshotVo implements Serializable {

    private static final long serialVersionUID = 9077719607175024038L;

    private String sourceCode;

    private String sourceName;

    private String fetchedAt;

    private String cachedAt;

    private Boolean stale;

    private List<HotItemVo> items = new ArrayList<>();
}
