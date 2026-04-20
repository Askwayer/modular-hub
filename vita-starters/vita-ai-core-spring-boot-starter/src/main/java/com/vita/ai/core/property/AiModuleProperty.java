package com.vita.ai.core.property;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AiModuleProperty {

    private boolean enabled = true;

    private String provider;

    private String model;

    private Map<String, Object> options = new HashMap<>();
}
