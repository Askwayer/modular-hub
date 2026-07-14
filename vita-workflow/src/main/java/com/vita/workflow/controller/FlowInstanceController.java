package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowInstanceCreateDto;
import com.vita.workflow.flow.dto.FlowInstanceSearchDto;
import com.vita.workflow.flow.dto.FlowInstanceUpdateDto;
import com.vita.workflow.flow.service.IFlowInstanceService;
import com.vita.workflow.flow.vo.FlowInstanceDetailVo;
import com.vita.workflow.flow.vo.FlowInstanceListVo;
import com.vita.workflow.flow.vo.FlowInstancePageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程实例表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowInstance")
public class FlowInstanceController extends BaseController {

    /**
     * 流程实例表 服务。
     */
    private final IFlowInstanceService flowInstanceService;

    public FlowInstanceController(IFlowInstanceService flowInstanceService) {
        this.flowInstanceService = flowInstanceService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowInstanceCreateDto createDto) {
        return CommonResult.success(flowInstanceService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowInstanceUpdateDto updateDto) {
        flowInstanceService.update(updateDto);
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
        flowInstanceService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowInstanceDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowInstanceService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowInstanceListVo>> list(@Valid FlowInstanceSearchDto searchDto) {
        return CommonResult.success(flowInstanceService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowInstancePageVo>> page(@Valid FlowInstanceSearchDto searchDto) {
        return CommonResult.success(flowInstanceService.page(searchDto));
    }
}
