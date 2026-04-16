package com.vita.aichat.enums;

/**
 * AI 请求生命周期状态枚举。
 */
public enum AiChatRequestStatusEnum {
    /* 等待处理 */
    PENDING,
    /* 流式输出中 */
    STREAMING,
    /* 成功完成 */
    SUCCESS,
    /* 请求失败 */
    ERROR,
    /* 已取消 */
    CANCELLED
}
