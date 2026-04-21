package com.vita.aichat.chat.model.context;

import lombok.Builder;
import lombok.Value;

/**
 * 已解析的上下文预算。
 */
@Value
@Builder
public class AiChatContextBudget {
    int promptBudgetTokens;
    int reserveOutputTokens;
    int safetyMarginTokens;
    int longMemoryMaxTokens;
    int carryForwardMaxTokens;
    int maxRoundsHardLimit;
}
