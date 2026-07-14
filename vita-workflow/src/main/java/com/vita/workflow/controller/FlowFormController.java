package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowFormCreateDto;
import com.vita.workflow.flow.dto.FlowFormSearchDto;
import com.vita.workflow.flow.dto.FlowFormUpdateDto;
import com.vita.workflow.flow.service.IFlowFormService;
import com.vita.workflow.flow.vo.FlowFormDetailVo;
import com.vita.workflow.flow.vo.FlowFormListVo;
import com.vita.workflow.flow.vo.FlowFormPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程表单表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowForm")
public class FlowFormController extends BaseController {

    /**
     * 流程表单表 服务。
     */
    private final IFlowFormService flowFormService;

    public FlowFormController(IFlowFormService flowFormService) {
        this.flowFormService = flowFormService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowFormCreateDto createDto) {
        return CommonResult.success(flowFormService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowFormUpdateDto updateDto) {
        flowFormService.update(updateDto);
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
        flowFormService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowFormDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowFormService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowFormListVo>> list(@Valid FlowFormSearchDto searchDto) {
        return CommonResult.success(flowFormService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowFormPageVo>> page(@Valid FlowFormSearchDto searchDto) {
        return CommonResult.success(flowFormService.page(searchDto));
    }
}
