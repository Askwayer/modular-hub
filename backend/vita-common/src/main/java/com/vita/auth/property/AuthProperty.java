package com.vita.auth.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.property
 * @Author: znk
 * @CreateTime: 2026-07-02
 * @Description: 认证白名单扩展配置
 * @Version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "vita.auth")
public class AuthProperty {

    /**
     * Sa-Token 认证放行路径扩展。
     */
    private List<String> extraExcludePaths = new ArrayList<>();
}
