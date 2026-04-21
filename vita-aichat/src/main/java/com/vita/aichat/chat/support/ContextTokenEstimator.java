package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.ai.core.model.AiChatMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 粗粒度 prompt token 估算器。
 */
@Component
public class ContextTokenEstimator {

    public int estimateText(String text) {
        if (CharSequenceUtil.isBlank(text)) {
            return 0;
        }
        int chineseChars = 0;
        int asciiChars = 0;
        int otherChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                continue;
            }
            Character.UnicodeBlock block = Character.UnicodeBlock.of(current);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                chineseChars++;
            } else if (current < 128) {
                asciiChars++;
            } else {
                otherChars++;
            }
        }
        return chineseChars + (int) Math.ceil(asciiChars / 4.0D) + otherChars + 4;
    }

    public int estimateMessage(AiChatMessage message) {
        if (message == null) {
            return 0;
        }
        return 6 + estimateText(message.getRole()) + estimateText(message.getContent());
    }

    public int estimateMessages(List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream().mapToInt(this::estimateMessage).sum();
    }
}
