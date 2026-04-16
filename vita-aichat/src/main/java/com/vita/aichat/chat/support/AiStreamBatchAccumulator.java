package com.vita.aichat.chat.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.aichat.chat.model.result.AiChatStreamBatchSnapshot;

/**
 * 流式批次聚合器。
 */
public class AiStreamBatchAccumulator {

    private final long flushIntervalMillis;
    private final int charThreshold;

    private final StringBuilder contentBuffer = new StringBuilder();
    private final StringBuilder reasoningBuffer = new StringBuilder();

    private long lastFlushAtMillis;

    public AiStreamBatchAccumulator(long flushIntervalMillis, int charThreshold, long startAtMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
        this.charThreshold = charThreshold;
        this.lastFlushAtMillis = startAtMillis;
    }

    public void append(String contentDelta, String reasoningDelta) {
        if (CharSequenceUtil.isNotBlank(contentDelta)) {
            contentBuffer.append(contentDelta);
        }
        if (CharSequenceUtil.isNotBlank(reasoningDelta)) {
            reasoningBuffer.append(reasoningDelta);
        }
    }

    public boolean hasPending() {
        return pendingChars() > 0;
    }

    public int pendingChars() {
        return contentBuffer.length() + reasoningBuffer.length();
    }

    public boolean shouldFlush(long nowMillis) {
        if (!hasPending()) {
            return false;
        }
        return pendingChars() >= charThreshold || nowMillis - lastFlushAtMillis >= flushIntervalMillis;
    }

    public boolean reachedCharThreshold() {
        return pendingChars() >= charThreshold;
    }

    public AiChatStreamBatchSnapshot drain(long nowMillis) {
        AiChatStreamBatchSnapshot snapshot = new AiChatStreamBatchSnapshot(
                contentBuffer.toString(),
                reasoningBuffer.toString(),
                pendingChars()
        );
        contentBuffer.setLength(0);
        reasoningBuffer.setLength(0);
        lastFlushAtMillis = nowMillis;
        return snapshot;
    }
}
