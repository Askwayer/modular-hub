package com.vita.aichat.chat.model.result;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 一次批次落库快照。
 */
@Data
@AllArgsConstructor
public class AiChatStreamBatchSnapshot {

    private String contentDelta;

    private String reasoningDelta;

    private Integer deltaChars;
}
