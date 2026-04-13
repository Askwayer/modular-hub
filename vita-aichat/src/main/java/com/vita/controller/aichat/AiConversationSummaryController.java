package com.vita.controller.aichat;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummaryCreateDto;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummarySearchDto;
import com.vita.aichat.aiConversationSummary.dto.AiConversationSummaryUpdateDto;
import com.vita.aichat.aiConversationSummary.service.IAiConversationSummaryService;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryDetailVo;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryListVo;
import com.vita.aichat.aiConversationSummary.vo.AiConversationSummaryPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.aichat
 * @Author znk
 * @CreateTime 2026-04-13 21:05:48
 * @Description: AI会话摘要表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/aichat/aiConversationSummary")
public class AiConversationSummaryController extends BaseController {

    /**
     * AI会话摘要表 服务。
     */
    private final IAiConversationSummaryService aiConversationSummaryService;

    public AiConversationSummaryController(IAiConversationSummaryService aiConversationSummaryService) {
        this.aiConversationSummaryService = aiConversationSummaryService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid AiConversationSummaryCreateDto createDto) {
        return CommonResult.success(aiConversationSummaryService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid AiConversationSummaryUpdateDto updateDto) {
        aiConversationSummaryService.update(updateDto);
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
        aiConversationSummaryService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<AiConversationSummaryDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(aiConversationSummaryService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<AiConversationSummaryListVo>> list(@Valid AiConversationSummarySearchDto searchDto) {
        return CommonResult.success(aiConversationSummaryService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<AiConversationSummaryPageVo>> page(@Valid AiConversationSummarySearchDto searchDto) {
        return CommonResult.success(aiConversationSummaryService.page(searchDto));
    }
}
