package com.vita.aichat.gateway;

/**
 * 抽象正式 Chat 对模型的同步与流式调用。
 */
public interface AiModelGatewayService {

    /**
     * 同步请求模型并返回完整回复。
     */
    AiChatGatewayResp chat(AiChatGatewayReq req);

    /**
     * 流式请求模型并通过监听器输出增量事件。
     */
    void chatStream(AiChatGatewayReq req, AiStreamListener listener);
}
