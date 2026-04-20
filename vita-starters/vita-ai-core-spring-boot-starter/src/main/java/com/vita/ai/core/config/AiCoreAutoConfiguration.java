package com.vita.ai.core.config;

import com.vita.ai.core.client.AiChatClient;
import com.vita.ai.core.client.DefaultAiChatClient;
import com.vita.ai.core.property.AiCoreProperty;
import com.vita.ai.core.provider.AiProvider;
import com.vita.ai.core.router.AiProviderRouter;
import com.vita.ai.core.router.DefaultAiProviderRouter;
import com.vita.ai.core.strategy.AiModuleStrategy;
import com.vita.ai.core.strategy.AiModuleStrategyRegistry;
import com.vita.ai.core.strategy.DefaultAiModuleStrategyRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(AiCoreProperty.class)
@ConditionalOnProperty(prefix = "vita.ai.core", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AiModuleStrategyRegistry.class)
    public AiModuleStrategyRegistry aiModuleStrategyRegistry(List<AiModuleStrategy> strategies) {
        return new DefaultAiModuleStrategyRegistry(strategies);
    }

    @Bean
    @ConditionalOnMissingBean(AiProviderRouter.class)
    public AiProviderRouter aiProviderRouter(List<AiProvider> providers) {
        return new DefaultAiProviderRouter(providers);
    }

    @Bean
    @ConditionalOnMissingBean(AiChatClient.class)
    public AiChatClient aiChatClient(AiModuleStrategyRegistry registry, AiProviderRouter router) {
        return new DefaultAiChatClient(registry, router);
    }
}
