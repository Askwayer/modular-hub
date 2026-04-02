package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuSearchDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysMenu.vo.SysMenuDetailVo;
import com.vita.system.sysMenu.vo.SysMenuListVo;
import com.vita.system.sysMenu.vo.SysMenuPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 控制器
 */
@Validated
@RestController
@RequestMapping("/system/sysMenu")
public class SysMenuController extends BaseController {

    /**
     * 菜单表 服务。
     */
    private final ISysMenuService sysMenuService;

    public SysMenuController(ISysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysMenuCreateDto createDto) {
        return CommonResult.success(sysMenuService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysMenuUpdateDto updateDto) {
        sysMenuService.update(updateDto);
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
        sysMenuService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysMenuDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysMenuService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysMenuListVo>> list(@Valid SysMenuSearchDto searchDto) {
        return CommonResult.success(sysMenuService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysMenuPageVo>> page(@Valid SysMenuSearchDto searchDto) {
        return CommonResult.success(sysMenuService.page(searchDto));
    }
}
