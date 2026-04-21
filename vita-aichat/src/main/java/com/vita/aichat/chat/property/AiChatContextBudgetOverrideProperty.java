package com.vita.aichat.chat.property;

import lombok.Data;

/**
 * 单模型上下文预算覆写。
 */
@Data
public class AiChatContextBudgetOverrideProperty {

    private Integer promptBudgetTokens;

    private Integer reserveOutputTokens;

    private Integer safetyMarginTokens;

    private Integer longMemoryMaxTokens;

    private Integer carryForwardMaxTokens;

    private Integer maxRoundsHardLimit;
}
