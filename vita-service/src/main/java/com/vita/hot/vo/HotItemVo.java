package com.vita.hot.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.vo
 * @Author: Codex
 * @CreateTime: 2026-05-11
 * @Description: 热榜条目视图对象
 * @Version: 1.0
 */
@Data
public class HotItemVo implements Serializable {

    private Integer rank;

    private String title;

    private String url;

    private String hotValue;

    private String cover;

    private String summary;

    private LocalDateTime publishedAt;
}
