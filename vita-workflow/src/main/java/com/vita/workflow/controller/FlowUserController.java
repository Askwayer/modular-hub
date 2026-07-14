package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.workflow.flow.dto.FlowUserCreateDto;
import com.vita.workflow.flow.dto.FlowUserSearchDto;
import com.vita.workflow.flow.dto.FlowUserUpdateDto;
import com.vita.workflow.flow.service.IFlowUserService;
import com.vita.workflow.flow.vo.FlowUserDetailVo;
import com.vita.workflow.flow.vo.FlowUserListVo;
import com.vita.workflow.flow.vo.FlowUserPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.workflow
 * @Author znk
 * @CreateTime 2026-07-14 15:51:28
 * @Description: 流程用户表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/workflow/flowUser")
public class FlowUserController extends BaseController {

    /**
     * 流程用户表 服务。
     */
    private final IFlowUserService flowUserService;

    public FlowUserController(IFlowUserService flowUserService) {
        this.flowUserService = flowUserService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid FlowUserCreateDto createDto) {
        return CommonResult.success(flowUserService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid FlowUserUpdateDto updateDto) {
        flowUserService.update(updateDto);
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
        flowUserService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<FlowUserDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(flowUserService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<FlowUserListVo>> list(@Valid FlowUserSearchDto searchDto) {
        return CommonResult.success(flowUserService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<FlowUserPageVo>> page(@Valid FlowUserSearchDto searchDto) {
        return CommonResult.success(flowUserService.page(searchDto));
    }
}
