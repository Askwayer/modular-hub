package com.vita.aichat.chat.model.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次批次落库快照。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiChatStreamBatchSnapshot {

    /**
     * 内容增量
     */
    private String contentDelta;

    /**
     * 推理内容增量
     */
    private String reasoningDelta;

    /**
     * 增量字符数
     */
    private Integer deltaChars;
}
