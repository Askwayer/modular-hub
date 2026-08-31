package com.vita.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Vita App启动类
 * @Version: 1.0
 */
@MapperScan("com.vita.**.mapper")
@SpringBootApplication(scanBasePackages = "com.vita")
public class VitaAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaAppApplication.class, args);
    }
}
