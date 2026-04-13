package com.vita.controller.aichat;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.aichat.aiMessage.dto.AiMessageCreateDto;
import com.vita.aichat.aiMessage.dto.AiMessageSearchDto;
import com.vita.aichat.aiMessage.dto.AiMessageUpdateDto;
import com.vita.aichat.aiMessage.service.IAiMessageService;
import com.vita.aichat.aiMessage.vo.AiMessageDetailVo;
import com.vita.aichat.aiMessage.vo.AiMessageListVo;
import com.vita.aichat.aiMessage.vo.AiMessagePageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.aichat
 * @Author znk
 * @CreateTime 2026-04-13 21:06:04
 * @Description: AI消息表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/aichat/aiMessage")
public class AiMessageController extends BaseController {

    /**
     * AI消息表 服务。
     */
    private final IAiMessageService aiMessageService;

    public AiMessageController(IAiMessageService aiMessageService) {
        this.aiMessageService = aiMessageService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid AiMessageCreateDto createDto) {
        return CommonResult.success(aiMessageService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid AiMessageUpdateDto updateDto) {
        aiMessageService.update(updateDto);
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
        aiMessageService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<AiMessageDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(aiMessageService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<AiMessageListVo>> list(@Valid AiMessageSearchDto searchDto) {
        return CommonResult.success(aiMessageService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<AiMessagePageVo>> page(@Valid AiMessageSearchDto searchDto) {
        return CommonResult.success(aiMessageService.page(searchDto));
    }
}
