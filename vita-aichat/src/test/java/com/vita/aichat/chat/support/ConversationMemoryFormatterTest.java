package com.vita.aichat.chat.support;

import com.alibaba.fastjson2.JSON;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiRoundMemory.entity.AiRoundMemory;
import com.vita.aichat.chat.model.memory.AiMemoryPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryFormatterTest {

    @Test
    void shouldRenderStructuredLongAndShortMemory() {
        ConversationMemoryFormatter formatter = new ConversationMemoryFormatter();
        AiMemoryPayload payload = new AiMemoryPayload();
        payload.setSummaryText("用户偏好 Java，要求回答精简。");
        payload.setFacts(List.of("项目是 Maven 多模块"));
        payload.setPreferences(List.of("回答精简"));
        payload.setConstraints(List.of("使用 JDK 17"));

        AiConversationSummary summary = new AiConversationSummary();
        summary.setSummaryText(payload.getSummaryText());
        summary.setSummaryJson(JSON.toJSONString(payload));

        AiRoundMemory roundMemory = new AiRoundMemory();
        roundMemory.setMemoryText("当前轮次记忆");
        roundMemory.setMemoryJson(JSON.toJSONString(payload));

        assertThat(formatter.renderLongMemory(summary))
                .contains("长期记忆")
                .contains("项目是 Maven 多模块")
                .contains("回答精简");
        assertThat(formatter.renderRoundMemory(roundMemory))
                .contains("短期记忆")
                .contains("使用 JDK 17");
    }
}
