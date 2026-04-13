package com.vita.aichat.aiMessage.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.aichat.aiMessage.dto
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiMessageSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
