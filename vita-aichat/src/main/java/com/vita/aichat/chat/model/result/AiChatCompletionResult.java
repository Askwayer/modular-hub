package com.vita.aichat.chat.model.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型完成结果聚合。
 */
@Data
public class AiChatCompletionResult {

    private String providerRequestId;

    private String content;

    private String reasoningContent;

    private String finishReason;

    private Integer tokenInput;

    private Integer tokenOutput;

    private Integer tokenTotal;

    private LocalDateTime firstTokenAt;

    private LocalDateTime completedAt;

    private Integer streamBatchCount;
}
