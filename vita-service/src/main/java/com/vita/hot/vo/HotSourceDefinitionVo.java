package com.vita.hot.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.vo
 * @Author: Codex
 * @CreateTime: 2026-05-12
 * @Description: 热榜源定义对象
 * @Version: 1.0
 */
@Data
public class HotSourceDefinitionVo implements Serializable {

    private static final long serialVersionUID = 6466157288759310560L;

    private String sourceCode;

    private String routeCode;

    private String iconKey;

    private String sourceName;

    private String sourceType;

    private String description;

    private Boolean enabled;

    private Boolean defaultVisible;
}
