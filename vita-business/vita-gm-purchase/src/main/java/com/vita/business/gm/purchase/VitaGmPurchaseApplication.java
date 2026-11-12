package com.vita.business.gm.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * vita-gm-purchase 独立应用入口，仅扫描本业务包。
 */
@SpringBootApplication
public class VitaGmPurchaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaGmPurchaseApplication.class, args);
    }
}
