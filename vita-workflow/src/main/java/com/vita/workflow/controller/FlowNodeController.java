package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowNodeCreateDto;
import com.vita.workflow.flow.dto.FlowNodeSearchDto;
import com.vita.workflow.flow.dto.FlowNodeUpdateDto;
import com.vita.workflow.flow.service.IFlowNodeService;
import com.vita.workflow.flow.vo.FlowNodeDetailVo;
import com.vita.workflow.flow.vo.FlowNodeListVo;
import com.vita.workflow.flow.vo.FlowNodePageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程节点表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowNode")
public class FlowNodeController extends BaseController {

    /**
     * 流程节点表 服务。
     */
    private final IFlowNodeService flowNodeService;

    public FlowNodeController(IFlowNodeService flowNodeService) {
        this.flowNodeService = flowNodeService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowNodeCreateDto createDto) {
        return CommonResult.success(flowNodeService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowNodeUpdateDto updateDto) {
        flowNodeService.update(updateDto);
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
        flowNodeService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowNodeDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowNodeService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowNodeListVo>> list(@Valid FlowNodeSearchDto searchDto) {
        return CommonResult.success(flowNodeService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowNodePageVo>> page(@Valid FlowNodeSearchDto searchDto) {
        return CommonResult.success(flowNodeService.page(searchDto));
    }
}
