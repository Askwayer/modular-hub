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

    @PatchMapping("/conversations/{conversationId}/title")
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
