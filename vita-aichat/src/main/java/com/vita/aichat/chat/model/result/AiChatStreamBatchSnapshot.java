package com.vita.aichat.chat.model.result;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 一次批次落库快照。
 */
@Data
@AllArgsConstructor
public class AiChatStreamBatchSnapshot {

    /* 内容增量 */
    private String contentDelta;

    /* 推理内容增量 */
    private String reasoningDelta;

    /* 增量字符数 */
    private Integer deltaChars;
}
