package com.vita.aichat.error;

import com.vita.core.exception.ErrorCode;

/**
 * AI Chat 业务错误码。
 */
public interface AiChatErrorCode {

    ErrorCode CHAT_CONVERSATION_NOT_FOUND = new ErrorCode(2001, "会话不存在");
    ErrorCode CHAT_CONVERSATION_NOT_ACTIVE = new ErrorCode(2002, "当前会话不可发送消息");
    ErrorCode CHAT_MESSAGE_TYPE_UNSUPPORTED = new ErrorCode(2003, "暂不支持该消息类型");
    ErrorCode CHAT_REQUEST_DUPLICATE = new ErrorCode(2004, "请求ID重复，请勿重复提交");
    ErrorCode CHAT_GENERATING_CONFLICT = new ErrorCode(2005, "当前会话已有生成任务，请稍后重试");
    ErrorCode CHAT_STREAM_DISABLED = new ErrorCode(2006, "当前环境未开启流式回复");
    ErrorCode MODEL_AUTH_ERROR = new ErrorCode(2101, "模型鉴权失败");
    ErrorCode MODEL_TIMEOUT = new ErrorCode(2102, "模型响应超时");
    ErrorCode MODEL_RATE_LIMIT = new ErrorCode(2103, "模型请求过于频繁");
    ErrorCode MODEL_PROVIDER_ERROR = new ErrorCode(2104, "模型服务异常");
    ErrorCode MODEL_NETWORK_ERROR = new ErrorCode(2105, "模型网络异常");
    ErrorCode MODEL_RESPONSE_INVALID = new ErrorCode(2106, "模型返回内容无效");
}
