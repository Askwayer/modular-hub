package com.vita.controller.aichat;

import com.vita.aichat.chat.dto.*;
import com.vita.aichat.chat.service.IAiChatService;
import com.vita.aichat.chat.vo.AiChatSendVo;
import com.vita.aichat.chat.vo.AiConversationDetailVo;
import com.vita.aichat.chat.vo.AiConversationPageVo;
import com.vita.aichat.chat.vo.AiMessagePageVo;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Chat 控制器。
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
     * 创建会话。
     *
     * @param createDto 创建请求对象
     * @return 会话ID
     */
    @PostMapping("/conversations")
    public CommonResult<Long> createConversation(@RequestBody @Valid AiConversationCreateDto createDto) {
        return CommonResult.success(aiChatService.createConversation(createDto));
    }

    /**
     * 分页查询会话列表。
     *
     * @param queryDto 分页查询条件
     * @return 会话分页数据
     */
    @GetMapping("/conversations")
    public CommonResult<PageResponse<AiConversationPageVo>> pageConversations(@Valid AiConversationPageQueryDto queryDto) {
        return CommonResult.success(aiChatService.pageConversations(queryDto));
    }

    /**
     * 查询会话详情。
     *
     * @param conversationId 会话ID
     * @return 会话详情对象
     */
    @GetMapping("/conversations/{conversationId}")
    public CommonResult<AiConversationDetailVo> getConversation(@PathVariable("conversationId") Long conversationId) {
        return CommonResult.success(aiChatService.getConversation(conversationId));
    }

    /**
     * 重命名会话标题。
     *
     * @param conversationId 会话ID
     * @param updateDto      标题更新请求对象
     * @return 操作结果
     */
    @PostMapping("/conversations/{conversationId}/title")
    public CommonResult<Boolean> renameConversation(@PathVariable("conversationId") Long conversationId,
                                                    @RequestBody @Valid AiConversationTitleUpdateDto updateDto) {
        aiChatService.renameConversation(conversationId, updateDto);
        return CommonResult.success(Boolean.TRUE);
    }

    /**
     * 删除会话。
     *
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/conversations/{conversationId}")
    public CommonResult<Boolean> deleteConversation(@PathVariable("conversationId") Long conversationId) {
        aiChatService.deleteConversation(conversationId);
        return CommonResult.success(Boolean.TRUE);
    }

    /**
     * 分页查询消息列表。
     *
     * @param conversationId 会话ID
     * @param queryDto       分页查询条件
     * @return 消息分页数据
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public CommonResult<PageResponse<AiMessagePageVo>> pageMessages(@PathVariable("conversationId") Long conversationId,
                                                                    @Valid AiMessagePageQueryDto queryDto) {
        return CommonResult.success(aiChatService.pageMessages(conversationId, queryDto));
    }

    /**
     * 发送消息（同步模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @return 发送结果
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public CommonResult<AiChatSendVo> sendMessage(@PathVariable("conversationId") Long conversationId,
                                                  @RequestBody @Valid AiMessageSendDto sendDto) {
        return CommonResult.success(aiChatService.sendMessage(conversationId, sendDto));
    }

    /**
     * 发送消息（流式模式）。
     *
     * @param conversationId 会话ID
     * @param sendDto        消息发送请求对象
     * @return SSE流式响应
     */
    @PostMapping(value = "/conversations/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@PathVariable("conversationId") Long conversationId,
                                        @RequestBody @Valid AiMessageSendDto sendDto) {
        return aiChatService.sendMessageStream(conversationId, sendDto);
    }
}
