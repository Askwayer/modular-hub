package com.vita.aichat.chat.model.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型完成结果聚合。
 */
@Data
public class AiChatCompletionResult {

    /* 模型提供商请求ID */
    private String providerRequestId;

    /* 回复内容 */
    private String content;

    /* 推理过程内容 */
    private String reasoningContent;

    /* 结束原因 */
    private String finishReason;

    /* 输入Token数 */
    private Integer tokenInput;

    /* 输出Token数 */
    private Integer tokenOutput;

    /* 总Token数 */
    private Integer tokenTotal;

    /* 首字返回时间 */
    private LocalDateTime firstTokenAt;

    /* 完成时间 */
    private LocalDateTime completedAt;

    /* 流式批次数量 */
    private Integer streamBatchCount;
}
