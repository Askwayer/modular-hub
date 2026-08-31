package com.vita.app.oauth.support;

import com.vita.app.oauth.port.AppOAuthProvider;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.support
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: OAuth平台适配器注册表
 * @Version: 1.0
 */
@Component
public class AppOAuthProviderRegistry {

    private final Map<String, AppOAuthProvider> providers;

    public AppOAuthProviderRegistry(List<AppOAuthProvider> providers) {
        Map<String, AppOAuthProvider> registry = new HashMap<>();
        for (AppOAuthProvider provider : providers) {
            String code = normalize(provider.providerCode());
            if (registry.putIfAbsent(code, provider) != null) {
                throw new IllegalStateException("OAuth平台适配器编码重复：" + code);
            }
        }
        this.providers = Map.copyOf(registry);
    }

    /**
     * 只允许调用服务端已注册的平台实现，前端不能传入任意OAuth端点或函数名。
     *
     * @param providerCode 平台编码
     * @return 平台适配器
     */
    public AppOAuthProvider getRequired(String providerCode) {
        AppOAuthProvider provider = providers.get(normalize(providerCode));
        if (provider == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth平台未启用");
        }
        return provider;
    }

    private String normalize(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth平台编码不能为空");
        }
        return providerCode.trim().toLowerCase(Locale.ROOT);
    }
}
