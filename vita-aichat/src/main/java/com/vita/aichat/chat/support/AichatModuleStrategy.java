package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.exception.AiProviderNotFoundException;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.property.AiCoreProperty;
import com.vita.ai.core.property.AiModuleProperty;
import com.vita.ai.core.strategy.AiModuleStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * aichat 业务模块策略实现。
 */
@Component
@RequiredArgsConstructor
public class AichatModuleStrategy implements AiModuleStrategy {

    private static final String MODULE_CODE = "aichat";

    private final AiCoreProperty aiCoreProperty;

    @Override
    public String module() {
        return MODULE_CODE;
    }

    @Override
    public AiChatRequest prepare(AiChatRequest request) {
        AiModuleProperty moduleProperty = aiCoreProperty.getModules().get(MODULE_CODE);
        if (CharSequenceUtil.isBlank(request.getProvider())) {
            request.setProvider(moduleProperty != null ? moduleProperty.getDefaultProvider() : "qwen");
        }
        if (CharSequenceUtil.isBlank(request.getModel())) {
            request.setModel(moduleProperty != null ? moduleProperty.getDefaultModel() : "qwen-plus");
        }
        if (moduleProperty != null && moduleProperty.getOptions() != null && !moduleProperty.getOptions().isEmpty()) {
            if (request.getOptions() == null) {
                request.setOptions(new java.util.HashMap<>(moduleProperty.getOptions()));
            } else {
                moduleProperty.getOptions().forEach(request.getOptions()::putIfAbsent);
            }
        }
        List<String> allowedProviders = moduleProperty != null ? moduleProperty.getAllowedProviders() : null;
        if (allowedProviders != null && !allowedProviders.isEmpty()
                && CharSequenceUtil.isNotBlank(request.getProvider())
                && allowedProviders.stream().noneMatch(provider -> provider.equalsIgnoreCase(request.getProvider()))) {
            throw new AiProviderNotFoundException(request.getProvider());
        }
        return request;
    }
}
