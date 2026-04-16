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

    /**
     * 构造批次聚合器。
     *
     * @param flushIntervalMillis 刷新间隔（毫秒）
     * @param charThreshold 字符阈值
     * @param startAtMillis 起始时间戳（毫秒）
     */
    public AiStreamBatchAccumulator(long flushIntervalMillis, int charThreshold, long startAtMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
        this.charThreshold = charThreshold;
        this.lastFlushAtMillis = startAtMillis;
    }

    /**
     * 追加内容增量和推理内容增量。
     *
     * @param contentDelta 内容增量
     * @param reasoningDelta 推理内容增量
     */
    public void append(String contentDelta, String reasoningDelta) {
        if (CharSequenceUtil.isNotBlank(contentDelta)) {
            contentBuffer.append(contentDelta);
        }
        if (CharSequenceUtil.isNotBlank(reasoningDelta)) {
            reasoningBuffer.append(reasoningDelta);
        }
    }

    /**
     * 判断是否有待刷新的数据。
     *
     * @return 是否有待刷新数据
     */
    public boolean hasPending() {
        return pendingChars() > 0;
    }

    /**
     * 获取待刷新的字符总数。
     *
     * @return 字符总数
     */
    public int pendingChars() {
        return contentBuffer.length() + reasoningBuffer.length();
    }

    /**
     * 判断是否满足刷新条件。
     *
     * @param nowMillis 当前时间戳（毫秒）
     * @return 是否应该刷新
     */
    public boolean shouldFlush(long nowMillis) {
        if (!hasPending()) {
            return false;
        }
        return pendingChars() >= charThreshold || nowMillis - lastFlushAtMillis >= flushIntervalMillis;
    }

    /**
     * 判断是否达到字符阈值。
     *
     * @return 是否达到阈值
     */
    public boolean reachedCharThreshold() {
        return pendingChars() >= charThreshold;
    }

    /**
     * 清空缓冲区并返回当前批次快照。
     *
     * @param nowMillis 当前时间戳（毫秒）
     * @return 批次快照
     */
    public AiChatStreamBatchSnapshot drain(long nowMillis) {
        AiChatStreamBatchSnapshot snapshot = AiChatStreamBatchSnapshot.builder()
                .contentDelta(contentBuffer.toString())
                .reasoningDelta(reasoningBuffer.toString())
                .deltaChars(pendingChars())
                .build();
        contentBuffer.setLength(0);
        reasoningBuffer.setLength(0);
        lastFlushAtMillis = nowMillis;
        return snapshot;
    }
}
