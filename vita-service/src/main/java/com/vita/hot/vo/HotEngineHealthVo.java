package com.vita.hot.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.hot.vo
 * @Author: znk
 * @CreateTime: 2026-05-14  10:52:04
 * @Description: hot-engine 健康检查结果 VO
 * @Version: 1.0
 */
@Data
public class HotEngineHealthVo implements Serializable {

    private Boolean healthy;

    private String baseUrl;

    private Long responseTimeMs;

    private String message;
}