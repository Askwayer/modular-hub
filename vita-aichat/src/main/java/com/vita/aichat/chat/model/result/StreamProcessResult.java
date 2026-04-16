package com.vita.aichat.chat.model.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.aichat.chat.model.result
 * @Author: znk
 * @CreateTime: 2026-04-16  21:05:52
 * @Description: 流式处理结果模型，包含模型提供商请求ID、回复内容、推理过程内容、结束原因、Token统计和时间戳等信息。
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StreamProcessResult {

    /**
     * 模型提供商请求ID
     */
    private String providerRequestId;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 推理过程内容
     */
    private String reasoningContent;

    /**
     * 结束原因
     */
    private String finishReason;

    /**
     * 输入Token数
     */
    private Integer tokenInput;

    /**
     * 输出Token数
     */
    private Integer tokenOutput;

    /**
     * 总Token数
     */
    private Integer tokenTotal;

    /**
     * 首字返回时间
     */
    private LocalDateTime firstTokenAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 流式批次数量
     */
    private Integer streamBatchCount;
}
