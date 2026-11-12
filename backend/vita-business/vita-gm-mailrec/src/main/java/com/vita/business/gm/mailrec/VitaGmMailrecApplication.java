package com.vita.business.gm.mailrec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * vita-gm-mailrec 独立应用入口，仅扫描本业务包。
 */
@SpringBootApplication
public class VitaGmMailrecApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaGmMailrecApplication.class, args);
    }
}
