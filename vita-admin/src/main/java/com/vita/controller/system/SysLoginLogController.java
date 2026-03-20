package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.system.sysLoginLog.dto.SysLoginLogCreateDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogSearchDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogUpdateDto;
import com.vita.system.sysLoginLog.service.ISysLoginLogService;
import com.vita.system.sysLoginLog.vo.SysLoginLogDetailVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogListVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.vita.core.controller.BaseController;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-21 00:20:48
 * @Description: 系统访问记录 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysLoginLog")
public class SysLoginLogController extends BaseController {

    /**
     * 系统访问记录 服务。
     */
    private final ISysLoginLogService sysLoginLogService;

    public SysLoginLogController(ISysLoginLogService sysLoginLogService) {
        this.sysLoginLogService = sysLoginLogService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysLoginLogCreateDto createDto) {
        return CommonResult.success(sysLoginLogService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysLoginLogUpdateDto updateDto) {
        sysLoginLogService.update(updateDto);
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
        sysLoginLogService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysLoginLogDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysLoginLogService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysLoginLogListVo>> list(@Valid SysLoginLogSearchDto searchDto) {
        return CommonResult.success(sysLoginLogService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysLoginLogPageVo>> page(@Valid SysLoginLogSearchDto searchDto) {
        return CommonResult.success(sysLoginLogService.page(searchDto));
    }
}
