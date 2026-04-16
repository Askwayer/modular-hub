package com.vita.aichat.chat.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiConversationPageQueryDto extends PageRequest {

    private String keyword;
}
