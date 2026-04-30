package com.vita.aichat.chat.model.context;

import com.vita.core.model.LoginUserInfoModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebFlux 请求上下文快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatReactiveRequestContext {

    private String requestId;

    private LoginUserInfoModel loginUserInfo;
}
