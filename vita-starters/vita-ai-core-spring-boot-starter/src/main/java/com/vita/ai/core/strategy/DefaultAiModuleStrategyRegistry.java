package com.vita.ai.core.strategy;

import com.vita.ai.core.exception.AiModuleStrategyNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class DefaultAiModuleStrategyRegistry implements AiModuleStrategyRegistry {

    private final List<AiModuleStrategy> strategies;

    @Override
    public AiModuleStrategy get(String module) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(module))
                .findFirst()
                .orElseThrow(() -> new AiModuleStrategyNotFoundException(module));
    }
}
