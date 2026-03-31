package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionCreateDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionSearchDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionUpdateDto;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionDetailVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionListVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:40:54
 * @Description: 角色权限关联表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysRolePermission")
public class SysRolePermissionController extends BaseController {

    /**
     * 角色权限关联表 服务。
     */
    private final ISysRolePermissionService sysRolePermissionService;

    public SysRolePermissionController(ISysRolePermissionService sysRolePermissionService) {
        this.sysRolePermissionService = sysRolePermissionService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysRolePermissionCreateDto createDto) {
        return CommonResult.success(sysRolePermissionService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysRolePermissionUpdateDto updateDto) {
        sysRolePermissionService.update(updateDto);
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
        sysRolePermissionService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysRolePermissionDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysRolePermissionService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysRolePermissionListVo>> list(@Valid SysRolePermissionSearchDto searchDto) {
        return CommonResult.success(sysRolePermissionService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysRolePermissionPageVo>> page(@Valid SysRolePermissionSearchDto searchDto) {
        return CommonResult.success(sysRolePermissionService.page(searchDto));
    }
}
