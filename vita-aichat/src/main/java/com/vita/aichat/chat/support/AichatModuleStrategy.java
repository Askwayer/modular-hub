package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.model.AiChatRequest;
import com.vita.ai.core.strategy.AiModuleStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * aichat 业务模块策略实现。
 */
@Component
@RequiredArgsConstructor
public class AichatModuleStrategy implements AiModuleStrategy {

    @Value("${vita.aichat.default-provider:dashscope}")
    private String defaultProvider;

    @Value("${vita.aichat.default-model:qwen-plus}")
    private String defaultModel;

    @Override
    public String module() {
        return "aichat";
    }

    @Override
    public AiChatRequest prepare(AiChatRequest request) {
        if (CharSequenceUtil.isBlank(request.getProvider())) {
            request.setProvider(defaultProvider);
        }
        if (CharSequenceUtil.isBlank(request.getModel())) {
            request.setModel(defaultModel);
        }
        return request;
    }
}
