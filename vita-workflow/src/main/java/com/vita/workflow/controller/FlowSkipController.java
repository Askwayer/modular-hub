package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowSkipCreateDto;
import com.vita.workflow.flow.dto.FlowSkipSearchDto;
import com.vita.workflow.flow.dto.FlowSkipUpdateDto;
import com.vita.workflow.flow.service.IFlowSkipService;
import com.vita.workflow.flow.vo.FlowSkipDetailVo;
import com.vita.workflow.flow.vo.FlowSkipListVo;
import com.vita.workflow.flow.vo.FlowSkipPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 节点跳转关联表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowSkip")
public class FlowSkipController extends BaseController {

    /**
     * 节点跳转关联表 服务。
     */
    private final IFlowSkipService flowSkipService;

    public FlowSkipController(IFlowSkipService flowSkipService) {
        this.flowSkipService = flowSkipService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowSkipCreateDto createDto) {
        return CommonResult.success(flowSkipService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowSkipUpdateDto updateDto) {
        flowSkipService.update(updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除数据。
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @GetMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        flowSkipService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowSkipDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowSkipService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowSkipListVo>> list(@Valid FlowSkipSearchDto searchDto) {
        return CommonResult.success(flowSkipService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowSkipPageVo>> page(@Valid FlowSkipSearchDto searchDto) {
        return CommonResult.success(flowSkipService.page(searchDto));
    }
}
