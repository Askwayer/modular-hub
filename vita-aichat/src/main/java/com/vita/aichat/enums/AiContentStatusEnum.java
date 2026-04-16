package com.vita.aichat.enums;

/**
 * AI 消息内容状态枚举。
 */
public enum AiContentStatusEnum {
    /* 正在生成 */
    GENERATING,
    /* 生成完成 */
    DONE,
    /* 生成失败 */
    ERROR,
    /* 已取消 */
    CANCELLED
}
