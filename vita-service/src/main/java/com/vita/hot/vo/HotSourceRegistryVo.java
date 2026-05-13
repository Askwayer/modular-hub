package com.vita.hot.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.vo
 * @Author: Codex
 * @CreateTime: 2026-05-12
 * @Description: 热榜源目录缓存对象
 * @Version: 1.0
 */
@Data
public class HotSourceRegistryVo implements Serializable {

    private static final long serialVersionUID = -1191432186104750561L;

    private String syncedAt;

    private List<HotSourceDefinitionVo> sources = new ArrayList<>();
}
