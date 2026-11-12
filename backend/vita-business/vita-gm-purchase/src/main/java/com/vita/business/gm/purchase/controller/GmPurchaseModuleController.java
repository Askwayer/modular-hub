package com.vita.business.gm.purchase.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块名称接口，用于验证独立应用的打包与部署结果。
 */
@RestController
@RequestMapping("/module")
public class GmPurchaseModuleController {

    @GetMapping(value = "/name", produces = MediaType.TEXT_PLAIN_VALUE)
    public String name() {
        return "vita-gm-purchase";
    }
}
