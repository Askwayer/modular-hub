package com.vita.controller.aichat;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.aichat.aiConversation.dto.AiConversationCreateDto;
import com.vita.aichat.aiConversation.dto.AiConversationSearchDto;
import com.vita.aichat.aiConversation.dto.AiConversationUpdateDto;
import com.vita.aichat.aiConversation.service.IAiConversationService;
import com.vita.aichat.aiConversation.vo.AiConversationDetailVo;
import com.vita.aichat.aiConversation.vo.AiConversationListVo;
import com.vita.aichat.aiConversation.vo.AiConversationPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.aichat
 * @Author znk
 * @CreateTime 2026-04-13 21:05:23
 * @Description: AI会话表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/aichat/aiConversation")
public class AiConversationController extends BaseController {

    /**
     * AI会话表 服务。
     */
    private final IAiConversationService aiConversationService;

    public AiConversationController(IAiConversationService aiConversationService) {
        this.aiConversationService = aiConversationService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid AiConversationCreateDto createDto) {
        return CommonResult.success(aiConversationService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid AiConversationUpdateDto updateDto) {
        aiConversationService.update(updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除数据。
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        aiConversationService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<AiConversationDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(aiConversationService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<AiConversationListVo>> list(@Valid AiConversationSearchDto searchDto) {
        return CommonResult.success(aiConversationService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<AiConversationPageVo>> page(@Valid AiConversationSearchDto searchDto) {
        return CommonResult.success(aiConversationService.page(searchDto));
    }
}
