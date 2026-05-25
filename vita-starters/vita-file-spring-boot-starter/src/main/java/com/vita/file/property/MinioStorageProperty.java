package com.vita.file.property;

import lombok.Data;

/**
 * MinIO 存储配置。
 */
@Data
public class MinioStorageProperty {

    /**
     * MinIO endpoint 地址。
     */
    private String endpoint = "http://127.0.0.1:9000";

    /**
     * MinIO access key。
     */
    private String accessKey = "minioadmin";

    /**
     * MinIO secret key。
     */
    private String secretKey = "minioadmin";

    /**
     * MinIO bucket 名称。
     */
    private String bucket = "vita-file";

    /**
     * MinIO 公开访问地址前缀。
     */
    private String publicDomain = "http://127.0.0.1:9000";

    /**
     * MinIO 私有访问地址前缀，留空时回退到 publicDomain。
     */
    private String privateDomain;
}
