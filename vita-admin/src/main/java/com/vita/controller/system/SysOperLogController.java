package com.vita.controller.system;

import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.system.sysOperLog.dto.SysOperLogCreateDto;
import com.vita.system.sysOperLog.dto.SysOperLogSearchDto;
import com.vita.system.sysOperLog.dto.SysOperLogUpdateDto;
import com.vita.system.sysOperLog.service.ISysOperLogService;
import com.vita.system.sysOperLog.vo.SysOperLogDetailVo;
import com.vita.system.sysOperLog.vo.SysOperLogListVo;
import com.vita.system.sysOperLog.vo.SysOperLogPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-12 21:45:20
 * @Description: 系统操作日志表 控制器
 */
@Validated
@RestController
@RequestMapping("/system/sysOperLog")
public class SysOperLogController extends BaseController {

    /**
     * 系统操作日志表 服务。
     */
    private final ISysOperLogService sysOperLogService;

    public SysOperLogController(ISysOperLogService sysOperLogService) {
        this.sysOperLogService = sysOperLogService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @PostMapping("/add")
    public CommonResult<Long> create(@RequestBody @Valid SysOperLogCreateDto createDto) {
        return CommonResult.success(sysOperLogService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysOperLogUpdateDto updateDto) {
        sysOperLogService.update(updateDto);
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
        sysOperLogService.delete(id);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    public CommonResult<SysOperLogDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysOperLogService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    public CommonResult<List<SysOperLogListVo>> list(@Valid SysOperLogSearchDto searchDto) {
        return CommonResult.success(sysOperLogService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    public CommonResult<PageResponse<SysOperLogPageVo>> page(@Valid SysOperLogSearchDto searchDto) {
        return CommonResult.success(sysOperLogService.page(searchDto));
    }
}
