package com.vita.aichat.chat.support;

import com.vita.aichat.aiConversation.entity.AiConversation;
import com.vita.aichat.chat.model.context.AiChatContextBudget;
import com.vita.aichat.chat.property.AiChatContextBudgetOverrideProperty;
import com.vita.aichat.chat.property.AiChatProperty;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextBudgetPolicyTest {

    @Test
    void shouldResolveModelOverrideAndConversationHardLimit() {
        AiChatProperty property = new AiChatProperty();
        AiChatContextBudgetOverrideProperty overrideProperty = new AiChatContextBudgetOverrideProperty();
        overrideProperty.setPromptBudgetTokens(7200);
        overrideProperty.setReserveOutputTokens(2600);
        overrideProperty.setMaxRoundsHardLimit(5);
        property.getContext().setModelBudgets(Map.of("qwen", Map.of("qwen-plus", overrideProperty)));

        AiConversation conversation = new AiConversation();
        conversation.setMaxContextMessages(8);

        ContextBudgetPolicy budgetPolicy = new ContextBudgetPolicy(property);
        AiChatContextBudget budget = budgetPolicy.resolve(conversation, "qwen", "qwen-plus");

        assertThat(budget.getPromptBudgetTokens()).isEqualTo(7200);
        assertThat(budget.getReserveOutputTokens()).isEqualTo(2600);
        assertThat(budget.getMaxRoundsHardLimit()).isEqualTo(4);
    }
}
