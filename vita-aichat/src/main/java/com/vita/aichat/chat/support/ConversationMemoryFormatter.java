package com.vita.aichat.chat.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.vita.aichat.aiConversationSummary.entity.AiConversationSummary;
import com.vita.aichat.aiRoundMemory.entity.AiRoundMemory;
import com.vita.aichat.chat.model.memory.AiMemoryPayload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化记忆格式化器。
 */
@Component
public class ConversationMemoryFormatter {

    public AiMemoryPayload parsePayload(String json) {
        if (CharSequenceUtil.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parseObject(stripMarkdownFence(json), AiMemoryPayload.class);
        } catch (JSONException ex) {
            return null;
        }
    }

    public String renderLongMemory(AiConversationSummary summary) {
        if (summary == null) {
            return null;
        }
        String compact = renderStructuredMemory(parsePayload(summary.getSummaryJson()),
                CharSequenceUtil.blankToDefault(summary.getSummaryText(), ""));
        return CharSequenceUtil.isBlank(compact) ? null : "以下是当前会话的长期记忆：\n" + compact;
    }

    public String renderRoundMemory(AiRoundMemory roundMemory) {
        if (roundMemory == null) {
            return null;
        }
        String compact = renderStructuredMemory(parsePayload(roundMemory.getMemoryJson()),
                CharSequenceUtil.blankToDefault(roundMemory.getMemoryText(), ""));
        return CharSequenceUtil.isBlank(compact) ? null : "以下是需要延续的短期记忆：\n" + compact;
    }

    private String renderStructuredMemory(AiMemoryPayload payload, String fallbackText) {
        List<String> parts = new ArrayList<>();
        if (payload != null) {
            appendSection(parts, "事实", payload.getFacts());
            appendSection(parts, "偏好", payload.getPreferences());
            appendSection(parts, "约束", payload.getConstraints());
            appendSection(parts, "待解决事项", payload.getOpenLoops());
            appendSection(parts, "待办", payload.getPendingTasks());
            appendSection(parts, "禁忌", payload.getDisallowed());
            if (CharSequenceUtil.isNotBlank(payload.getCarryForwardReason())) {
                parts.add("延续原因：" + payload.getCarryForwardReason().trim());
            }
            String summaryText = CharSequenceUtil.isNotBlank(payload.getSummaryText())
                    ? payload.getSummaryText()
                    : payload.getMemoryText();
            if (parts.isEmpty() && CharSequenceUtil.isNotBlank(summaryText)) {
                parts.add(summaryText.trim());
            }
        }
        if (parts.isEmpty() && CharSequenceUtil.isNotBlank(fallbackText)) {
            parts.add(fallbackText.trim());
        }
        return String.join("\n", parts);
    }

    private void appendSection(List<String> parts, String title, List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return;
        }
        List<String> normalizedValues = values.stream()
                .filter(CharSequenceUtil::isNotBlank)
                .map(String::trim)
                .toList();
        if (normalizedValues.isEmpty()) {
            return;
        }
        parts.add(title + "：" + String.join("；", normalizedValues));
    }

    private String stripMarkdownFence(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineBreak = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineBreak < 0 || lastFence <= firstLineBreak) {
            return trimmed;
        }
        return trimmed.substring(firstLineBreak + 1, lastFence).trim();
    }
}
