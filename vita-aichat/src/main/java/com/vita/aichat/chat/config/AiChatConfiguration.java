package com.vita.aichat.chat.config;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.aichat.error.AiChatErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AI Chat 配置。
 */
@Configuration
public class AiChatConfiguration {

    @Bean(name = "aiChatExecutor")
    public Executor aiChatExecutor(@Qualifier("ThreadPoolTaskExecutor") ThreadPoolTaskExecutor executor) {
        return executor;
    }

    @Bean
    public InitializingBean aiChatStartupValidator(AiChatProperties properties) {
        return () -> {
            if (!"dashscope".equalsIgnoreCase(properties.getProvider())) {
                throw new ServiceException(AiChatErrorCode.MODEL_PROVIDER_ERROR.getCode(), "spring.ai.model.chat 仅支持 dashscope");
            }
            if (CharSequenceUtil.isBlank(properties.getDashscopeApiKey())) {
                throw new ServiceException(AiChatErrorCode.MODEL_AUTH_ERROR.getCode(), "spring.ai.dashscope.api-key 未配置");
            }
            if (CharSequenceUtil.isBlank(properties.getDashscopeBaseUrl())) {
                throw new ServiceException(AiChatErrorCode.MODEL_PROVIDER_ERROR.getCode(), "spring.ai.dashscope.base-url 未配置");
            }
            if (CharSequenceUtil.isBlank(properties.getDashscopeChatModel())) {
                throw new ServiceException(AiChatErrorCode.MODEL_PROVIDER_ERROR.getCode(), "spring.ai.dashscope.chat.options.model 未配置");
            }
        };
    }
}
