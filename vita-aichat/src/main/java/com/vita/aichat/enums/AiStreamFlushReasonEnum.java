package com.vita.aichat.enums;

/**
 * AI 流式批次落库原因枚举。
 */
public enum AiStreamFlushReasonEnum {
    /* 时间窗口触发 */
    TIME_WINDOW,
    /* 字符阈值触发 */
    CHAR_THRESHOLD,
    /* 最终落库 */
    FINAL,
    /* 错误触发 */
    ERROR
}
