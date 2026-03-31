package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleCreateDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleSearchDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleUpdateDto;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleDetailVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleListVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRolePageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:39:20
 * @Description: 用户角色关联表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysUserRole")
public class SysUserRoleController extends BaseController {

    /**
     * 用户角色关联表 服务。
     */
    private final ISysUserRoleService sysUserRoleService;

    public SysUserRoleController(ISysUserRoleService sysUserRoleService) {
        this.sysUserRoleService = sysUserRoleService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysUserRoleCreateDto createDto) {
        return CommonResult.success(sysUserRoleService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysUserRoleUpdateDto updateDto) {
        sysUserRoleService.update(updateDto);
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
        sysUserRoleService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysUserRoleDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysUserRoleService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysUserRoleListVo>> list(@Valid SysUserRoleSearchDto searchDto) {
        return CommonResult.success(sysUserRoleService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysUserRolePageVo>> page(@Valid SysUserRoleSearchDto searchDto) {
        return CommonResult.success(sysUserRoleService.page(searchDto));
    }
}
