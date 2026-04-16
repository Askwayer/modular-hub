package com.vita.aichat.chat.config;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.aichat.chat.property.AiChatProperty;
import com.vita.aichat.error.AiChatErrorCode;
import com.vita.core.exception.ServiceException;
import jakarta.annotation.Resource;
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

    @Resource
    private AiChatProperty aiChatProperty;

    /**
     * 配置 AI Chat 异步执行器。
     *
     * @param executor 线程池任务执行器
     * @return 异步执行器
     */
    @Bean(name = "aiChatExecutor")
    public Executor aiChatExecutor(@Qualifier("threadPoolTaskExecutor") ThreadPoolTaskExecutor executor) {
        return executor;
    }

    /**
     * 配置 AI Chat 启动校验器，验证必要配置项。
     *
     * @return 初始化回调
     */
    @Bean
    public InitializingBean aiChatStartupValidator() {
        return () -> {
            if (!"dashscope".equalsIgnoreCase(aiChatProperty.getModelChat())) {
                throw new ServiceException(AiChatErrorCode.MODEL_PROVIDER_ERROR.getCode(), "spring.ai.model.chat 仅支持 dashscope");
            }
            if (CharSequenceUtil.isBlank(aiChatProperty.getDashscopeApiKey())) {
                throw new ServiceException(AiChatErrorCode.MODEL_AUTH_ERROR.getCode(), "spring.ai.dashscope.api-key 未配置");
            }
            if (CharSequenceUtil.isBlank(aiChatProperty.getDashscopeBaseUrl())) {
                throw new ServiceException(AiChatErrorCode.MODEL_PROVIDER_ERROR.getCode(), "spring.ai.dashscope.base-url 未配置");
            }
            if (CharSequenceUtil.isBlank(aiChatProperty.getDashscopeChatModel())) {
                throw new ServiceException(AiChatErrorCode.MODEL_PROVIDER_ERROR.getCode(), "spring.ai.dashscope.chat.options.model 未配置");
            }
        };
    }
}
