package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.sysPermission.dto.SysPermissionCreateDto;
import com.vita.system.sysPermission.dto.SysPermissionSearchDto;
import com.vita.system.sysPermission.dto.SysPermissionUpdateDto;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysPermission.vo.SysPermissionDetailVo;
import com.vita.system.sysPermission.vo.SysPermissionListVo;
import com.vita.system.sysPermission.vo.SysPermissionPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysPermission")
public class SysPermissionController extends BaseController {

    /**
     * 权限表 服务。
     */
    private final ISysPermissionService sysPermissionService;

    public SysPermissionController(ISysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysPermissionCreateDto createDto) {
        return CommonResult.success(sysPermissionService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysPermissionUpdateDto updateDto) {
        sysPermissionService.update(updateDto);
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
        sysPermissionService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysPermissionDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysPermissionService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysPermissionListVo>> list(@Valid SysPermissionSearchDto searchDto) {
        return CommonResult.success(sysPermissionService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysPermissionPageVo>> page(@Valid SysPermissionSearchDto searchDto) {
        return CommonResult.success(sysPermissionService.page(searchDto));
    }
}
