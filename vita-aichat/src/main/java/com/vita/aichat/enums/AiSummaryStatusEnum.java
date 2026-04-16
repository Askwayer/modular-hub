package com.vita.aichat.enums;

/**
 * AI 会话摘要状态枚举。
 */
public enum AiSummaryStatusEnum {
    /* 无摘要 */
    NONE,
    /* 准备就绪 */
    READY,
    /* 已过期 */
    STALE,
    /* 正在生成 */
    GENERATING
}
