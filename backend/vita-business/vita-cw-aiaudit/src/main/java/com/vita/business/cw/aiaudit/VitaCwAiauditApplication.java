package com.vita.business.cw.aiaudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * vita-cw-aiaudit 独立应用入口，仅扫描本业务包。
 */
@SpringBootApplication
public class VitaCwAiauditApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaCwAiauditApplication.class, args);
    }
}
