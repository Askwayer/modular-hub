package com.vita.controller.publicapi;

import com.vita.annotation.RepeatSubmit;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.vo.HotSourceStatusVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.publicapi
 * @Author: znk
 * @CreateTime: 2026-05-13 21:17:11
 * @Description: 热榜刷新公开接口控制器
 * @Version: 1.0
 */
@RestController
@RequestMapping("/public/hot")
public class HotRefreshPublicController extends BaseController {

    private final IHotSourceService hotSourceService;

    public HotRefreshPublicController(IHotSourceService hotSourceService) {
        this.hotSourceService = hotSourceService;
    }

    /**
     * 刷新全部启用热榜缓存。
     *
     * @return 刷新后的状态列表
     */
    @RepeatSubmit
    @PostMapping("/refreshAll")
    public CommonResult<List<HotSourceStatusVo>> refreshAll() {
        return CommonResult.success(hotSourceService.refreshAllEnabledSourcesWithFallback());
    }
}
