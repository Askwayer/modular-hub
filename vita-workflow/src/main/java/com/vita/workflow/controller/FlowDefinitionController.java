package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowDefinitionCreateDto;
import com.vita.workflow.flow.dto.FlowDefinitionSearchDto;
import com.vita.workflow.flow.dto.FlowDefinitionUpdateDto;
import com.vita.workflow.flow.service.IFlowDefinitionService;
import com.vita.workflow.flow.vo.FlowDefinitionDetailVo;
import com.vita.workflow.flow.vo.FlowDefinitionListVo;
import com.vita.workflow.flow.vo.FlowDefinitionPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程定义表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowDefinition")
public class FlowDefinitionController extends BaseController {

    /**
     * 流程定义表 服务。
     */
    private final IFlowDefinitionService flowDefinitionService;

    public FlowDefinitionController(IFlowDefinitionService flowDefinitionService) {
        this.flowDefinitionService = flowDefinitionService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowDefinitionCreateDto createDto) {
        return CommonResult.success(flowDefinitionService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowDefinitionUpdateDto updateDto) {
        flowDefinitionService.update(updateDto);
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
        flowDefinitionService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowDefinitionDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowDefinitionService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowDefinitionListVo>> list(@Valid FlowDefinitionSearchDto searchDto) {
        return CommonResult.success(flowDefinitionService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowDefinitionPageVo>> page(@Valid FlowDefinitionSearchDto searchDto) {
        return CommonResult.success(flowDefinitionService.page(searchDto));
    }
}
