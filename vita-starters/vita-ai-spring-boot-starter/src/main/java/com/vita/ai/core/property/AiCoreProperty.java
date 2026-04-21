package com.vita.ai.core.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "vita.ai")
public class AiCoreProperty {

    private boolean enabled = true;

    private Map<String, AiModuleProperty> modules = new HashMap<>();
}
