package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowTaskCreateDto;
import com.vita.workflow.flow.dto.FlowTaskSearchDto;
import com.vita.workflow.flow.dto.FlowTaskUpdateDto;
import com.vita.workflow.flow.service.IFlowTaskService;
import com.vita.workflow.flow.vo.FlowTaskDetailVo;
import com.vita.workflow.flow.vo.FlowTaskListVo;
import com.vita.workflow.flow.vo.FlowTaskPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 待办任务表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowTask")
public class FlowTaskController extends BaseController {

    /**
     * 待办任务表 服务。
     */
    private final IFlowTaskService flowTaskService;

    public FlowTaskController(IFlowTaskService flowTaskService) {
        this.flowTaskService = flowTaskService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowTaskCreateDto createDto) {
        return CommonResult.success(flowTaskService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowTaskUpdateDto updateDto) {
        flowTaskService.update(updateDto);
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
        flowTaskService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowTaskDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowTaskService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowTaskListVo>> list(@Valid FlowTaskSearchDto searchDto) {
        return CommonResult.success(flowTaskService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowTaskPageVo>> page(@Valid FlowTaskSearchDto searchDto) {
        return CommonResult.success(flowTaskService.page(searchDto));
    }
}
