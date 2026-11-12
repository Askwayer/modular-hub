package com.vita.business.ah.waste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * vita-ah-waste 独立应用入口，仅扫描本业务包。
 */
@SpringBootApplication
public class VitaAhWasteApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaAhWasteApplication.class, args);
    }
}
