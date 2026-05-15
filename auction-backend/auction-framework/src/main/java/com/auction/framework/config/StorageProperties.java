package com.auction.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性。
 * <p>
 * 通过 storage.type 切换存储实现：
 * - local：本地磁盘存储（开发环境）
 * - minio：MinIO 对象存储（生产环境）
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 存储类型：local / minio */
    private String type = "local";

    /** 本地存储根目录 */
    private String localPath = "./uploads";

    /** 本地存储访问前缀（返回给前端拼接 URL 用） */
    private String localUrlPrefix = "/uploads";

    /** MinIO 配置 */
    private Minio minio = new Minio();

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "admin";
        private String secretKey = "admin123456";
        private String bucket = "auction";
    }
}
