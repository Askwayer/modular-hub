package com.vita.ai.core.exception;

public class AiProviderNotFoundException extends RuntimeException {

    public AiProviderNotFoundException(String providerCode) {
        super("未找到对应的 AI Provider: " + providerCode);
    }
}
