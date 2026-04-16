package com.vita.controller.aichat;

import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.vo.AiChatConversationDetailVo;
import com.vita.aichat.chat.vo.AiChatConversationPageVo;
import com.vita.aichat.chat.vo.AiChatMessageVo;
import com.vita.aichat.chat.vo.AiChatSendRespVo;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 正式 Chat 接口入口，只暴露任务导向的会话和消息能力。
 */
@Validated
@RestController
@RequestMapping("/chat")
public class AiChatController extends BaseController {

    private final IAiChatService aiChatService;

    public AiChatController(IAiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 创建AI对话会话
     *
     * <p>根据提供的配置信息创建一个新的对话会话，包括会话标题、系统提示词、模型选择
     * 以及相关的推理参数（如温度、最大token数、上下文消息数等）。创建成功后返回新会话的唯一标识。</p>
     *
     * @param reqDto 会话创建请求对象，包含以下配置信息：
     *               - title: 会话标题（可选，长度不超过200字符）
     *               - systemPrompt: 系统提示词（可选，长度不超过4000字符）
     *               - modelCode: 模型编码（可选，长度不超过64字符）
     *               - temperature: 温度参数（可选，范围0-2，控制回复的随机性）
     *               - maxTokens: 最大输出token数（可选，范围1-65535）
     *               - maxContextMessages: 最大上下文消息数（可选，范围1-100）
     * @return 通用响应结果，数据部分为新创建会话的唯一标识ID
     */
    @PostMapping("/conversations")
    public CommonResult<Long> createConversation(@RequestBody @Valid AiChatConversationCreateReqDto reqDto) {
        return CommonResult.success(aiChatService.createConversation(reqDto));
    }

    @GetMapping("/conversations")
    public CommonResult<PageResponse<AiChatConversationPageVo>> pageConversations(@Valid AiChatConversationPageReqDto reqDto) {
        return CommonResult.success(aiChatService.pageConversations(reqDto));
    }

    @GetMapping("/conversations/{conversationId}")
    public CommonResult<AiChatConversationDetailVo> getConversation(@PathVariable("conversationId") Long conversationId) {
        return CommonResult.success(aiChatService.getConversation(conversationId));
    }

    /**
     * 重命名AI对话会话标题
     *
     * <p>根据会话ID修改指定会话的标题。此接口用于更新已存在会话的名称，
     * 便于用户管理和识别不同的对话会话。</p>
     *
     * <p><b>请求示例：</b></p>
     * <pre>
     * POST /chat/conversations/{conversationId}/title
     * Content-Type: application/json
     *
     * {
     *   "title": "新的会话标题"
     * }
     * </pre>
     *
     * @param conversationId 会话唯一标识ID（路径参数）
     * @param reqDto         会话重命名请求对象，包含：
     *                       - title: 新的会话标题（必填，长度不超过200字符）
     * @return 通用响应结果，数据部分为操作是否成功的布尔值
     */
    @PostMapping("/conversations/{conversationId}/title")
    public CommonResult<Boolean> renameConversation(@PathVariable("conversationId") Long conversationId,
                                                    @RequestBody @Valid AiChatConversationRenameReqDto reqDto) {
        aiChatService.renameConversation(conversationId, reqDto);
        return CommonResult.success(true);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public CommonResult<Boolean> deleteConversation(@PathVariable("conversationId") Long conversationId) {
        aiChatService.deleteConversation(conversationId);
        return CommonResult.success(true);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public CommonResult<PageResponse<AiChatMessageVo>> pageMessages(@PathVariable("conversationId") Long conversationId,
                                                                    @Valid AiChatMessagePageReqDto reqDto) {
        return CommonResult.success(aiChatService.pageMessages(conversationId, reqDto));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public CommonResult<AiChatSendRespVo> sendMessage(@PathVariable("conversationId") Long conversationId,
                                                      @RequestBody @Valid AiChatMessageSendReqDto reqDto) {
        return CommonResult.success(aiChatService.sendMessage(conversationId, reqDto));
    }

    @PostMapping(value = "/conversations/{conversationId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable("conversationId") Long conversationId,
                                    @RequestBody @Valid AiChatMessageSendReqDto reqDto) {
        return aiChatService.streamMessage(conversationId, reqDto);
    }
}
