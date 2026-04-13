package com.vita.aichat.aiConversation.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiConversation.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiConversationSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
