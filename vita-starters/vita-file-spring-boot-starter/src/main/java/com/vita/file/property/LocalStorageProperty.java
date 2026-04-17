package com.vita.file.property;

import lombok.Data;

/**
 * 本地存储配置。
 */
@Data
public class LocalStorageProperty {

    /**
     * 本地文件落盘根目录。
     */
    private String basePath = "/data/vita/uploads";
}
