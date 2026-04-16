package com.vita.aichat.chat.model.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Chat 全局并发名额。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatCapacityPermit {

    /* 活跃名额ID */
    private String activePermitId;

    /* 是否经过等待队列 */
    private boolean waited;
}
