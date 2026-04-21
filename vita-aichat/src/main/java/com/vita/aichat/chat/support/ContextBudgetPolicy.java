package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.chat.model.context.AiChatContextBudget;
import com.vita.aichat.chat.property.AiChatContextBudgetOverrideProperty;
import com.vita.aichat.chat.property.AiChatContextProperty;
import com.vita.aichat.chat.property.AiChatProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 解析最终生效的上下文预算。
 */
@Component
public class ContextBudgetPolicy {

    private final AiChatProperty aiChatProperty;

    public ContextBudgetPolicy(AiChatProperty aiChatProperty) {
        this.aiChatProperty = aiChatProperty;
    }

    public boolean useBudgetRoundAssembly() {
        return !"LEGACY".equalsIgnoreCase(aiChatProperty.getContext().getAssemblyMode());
    }

    public AiChatContextBudget resolve(AiConversation conversation, String providerCode, String modelCode) {
        AiChatContextProperty contextProperty = aiChatProperty.getContext();
        AiChatContextBudgetOverrideProperty overrideProperty = resolveOverride(contextProperty, providerCode, modelCode);
        Integer conversationRoundLimit = conversation == null ? null : toRoundHardLimit(conversation.getMaxContextMessages());
        int maxRoundsHardLimit = safePositive(
                conversationRoundLimit,
                safePositive(overrideValue(overrideProperty, AiChatContextBudgetOverrideProperty::getMaxRoundsHardLimit),
                        contextProperty.getMaxRoundsHardLimit())
        );
        return AiChatContextBudget.builder()
                .promptBudgetTokens(safePositive(
                        overrideValue(overrideProperty, AiChatContextBudgetOverrideProperty::getPromptBudgetTokens),
                        contextProperty.getPromptBudgetTokens()))
                .reserveOutputTokens(safePositive(
                        overrideValue(overrideProperty, AiChatContextBudgetOverrideProperty::getReserveOutputTokens),
                        contextProperty.getReserveOutputTokens()))
                .safetyMarginTokens(safePositive(
                        overrideValue(overrideProperty, AiChatContextBudgetOverrideProperty::getSafetyMarginTokens),
                        contextProperty.getSafetyMarginTokens()))
                .longMemoryMaxTokens(safePositive(
                        overrideValue(overrideProperty, AiChatContextBudgetOverrideProperty::getLongMemoryMaxTokens),
                        contextProperty.getLongMemoryMaxTokens()))
                .carryForwardMaxTokens(safePositive(
                        overrideValue(overrideProperty, AiChatContextBudgetOverrideProperty::getCarryForwardMaxTokens),
                        contextProperty.getCarryForwardMaxTokens()))
                .maxRoundsHardLimit(maxRoundsHardLimit)
                .build();
    }

    private AiChatContextBudgetOverrideProperty resolveOverride(AiChatContextProperty contextProperty,
                                                                String providerCode,
                                                                String modelCode) {
        if (contextProperty.getModelBudgets() == null
                || CharSequenceUtil.isBlank(providerCode)
                || CharSequenceUtil.isBlank(modelCode)) {
            return null;
        }
        Map<String, AiChatContextBudgetOverrideProperty> providerOverrides = contextProperty.getModelBudgets().get(providerCode);
        if (providerOverrides == null) {
            return null;
        }
        return providerOverrides.get(modelCode);
    }

    private Integer overrideValue(AiChatContextBudgetOverrideProperty overrideProperty,
                                  java.util.function.Function<AiChatContextBudgetOverrideProperty, Integer> getter) {
        return overrideProperty == null ? null : getter.apply(overrideProperty);
    }

    private int safePositive(Integer value, Integer fallback) {
        if (value != null && value > 0) {
            return value;
        }
        if (fallback != null && fallback > 0) {
            return fallback;
        }
        return 1;
    }

    private Integer toRoundHardLimit(Integer maxContextMessages) {
        if (maxContextMessages == null || maxContextMessages <= 0) {
            return null;
        }
        return Math.max(1, (maxContextMessages + 1) / 2);
    }
}
