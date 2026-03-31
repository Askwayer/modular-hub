package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuCreateDto;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuSearchDto;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuUpdateDto;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuDetailVo;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuListVo;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:40:21
 * @Description: 角色菜单关联表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysRoleMenu")
public class SysRoleMenuController extends BaseController {

    /**
     * 角色菜单关联表 服务。
     */
    private final ISysRoleMenuService sysRoleMenuService;

    public SysRoleMenuController(ISysRoleMenuService sysRoleMenuService) {
        this.sysRoleMenuService = sysRoleMenuService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysRoleMenuCreateDto createDto) {
        return CommonResult.success(sysRoleMenuService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysRoleMenuUpdateDto updateDto) {
        sysRoleMenuService.update(updateDto);
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
        sysRoleMenuService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysRoleMenuDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysRoleMenuService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysRoleMenuListVo>> list(@Valid SysRoleMenuSearchDto searchDto) {
        return CommonResult.success(sysRoleMenuService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysRoleMenuPageVo>> page(@Valid SysRoleMenuSearchDto searchDto) {
        return CommonResult.success(sysRoleMenuService.page(searchDto));
    }
}
