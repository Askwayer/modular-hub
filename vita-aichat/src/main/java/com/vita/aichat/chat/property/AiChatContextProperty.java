package com.vita.aichat.chat.property;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 上下文组装预算配置。
 */
@Data
public class AiChatContextProperty {

    /**
     * 上下文组装模式：LEGACY / BUDGET_ROUND。
     */
    private String assemblyMode = "BUDGET_ROUND";

    /**
     * Prompt 总预算，不含输出预留。
     */
    private Integer promptBudgetTokens = 6000;

    /**
     * 为模型输出预留的 token 数。
     */
    private Integer reserveOutputTokens = 2000;

    /**
     * 预算安全余量。
     */
    private Integer safetyMarginTokens = 500;

    /**
     * 长期记忆的最大注入预算。
     */
    private Integer longMemoryMaxTokens = 1200;

    /**
     * 短期 carry-forward 记忆的最大注入预算。
     */
    private Integer carryForwardMaxTokens = 1000;

    /**
     * 最近完整轮次的硬上限。
     */
    private Integer maxRoundsHardLimit = 6;

    /**
     * provider -> model -> budget override。
     */
    private Map<String, Map<String, AiChatContextBudgetOverrideProperty>> modelBudgets = new LinkedHashMap<>();
}
