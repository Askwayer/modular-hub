package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowHisTaskCreateDto;
import com.vita.workflow.flow.dto.FlowHisTaskSearchDto;
import com.vita.workflow.flow.dto.FlowHisTaskUpdateDto;
import com.vita.workflow.flow.service.IFlowHisTaskService;
import com.vita.workflow.flow.vo.FlowHisTaskDetailVo;
import com.vita.workflow.flow.vo.FlowHisTaskListVo;
import com.vita.workflow.flow.vo.FlowHisTaskPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 历史任务记录表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowHisTask")
public class FlowHisTaskController extends BaseController {

    /**
     * 历史任务记录表 服务。
     */
    private final IFlowHisTaskService flowHisTaskService;

    public FlowHisTaskController(IFlowHisTaskService flowHisTaskService) {
        this.flowHisTaskService = flowHisTaskService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowHisTaskCreateDto createDto) {
        return CommonResult.success(flowHisTaskService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowHisTaskUpdateDto updateDto) {
        flowHisTaskService.update(updateDto);
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
        flowHisTaskService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowHisTaskDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowHisTaskService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowHisTaskListVo>> list(@Valid FlowHisTaskSearchDto searchDto) {
        return CommonResult.success(flowHisTaskService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowHisTaskPageVo>> page(@Valid FlowHisTaskSearchDto searchDto) {
        return CommonResult.success(flowHisTaskService.page(searchDto));
    }
}
