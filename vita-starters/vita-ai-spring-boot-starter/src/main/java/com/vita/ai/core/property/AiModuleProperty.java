package com.vita.ai.core.property;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AiModuleProperty {

    private boolean enabled = true;

    private String defaultProvider;

    private String defaultModel;

    private List<String> allowedProviders;

    private Map<String, Object> options = new HashMap<>();
}
