package com.vita.ai.core.exception;

public class AiModuleStrategyNotFoundException extends RuntimeException {

    public AiModuleStrategyNotFoundException(String module) {
        super("未找到对应的 AI 模块策略: " + module);
    }
}
