package com.auction.framework.service.impl;

import com.auction.common.exception.BizException;
import com.auction.framework.config.StorageProperties;
import com.auction.framework.service.FileService;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * MinIO 对象存储实现。
 * 仅在 storage.type=minio 时激活。
 * <p>
 * 文件按 {biz}/{yyyy-MM-dd}/{uuid}.{ext} 存储在 MinIO bucket 中。
 * 返回的 URL 为 {endpoint}/{bucket}/{objectName}。
 * </p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "minio")
public class MinioFileServiceImpl implements FileService {

    private final StorageProperties properties;
    private MinioClient minioClient;

    public MinioFileServiceImpl(StorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 初始化 MinIO 客户端，自动创建 bucket（如果不存在）。
     */
    @PostConstruct
    public void init() {
        StorageProperties.Minio mc = properties.getMinio();
        this.minioClient = MinioClient.builder()
                .endpoint(mc.getEndpoint())
                .credentials(mc.getAccessKey(), mc.getSecretKey())
                .build();

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(mc.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(mc.getBucket()).build());
                log.info("MinIO bucket '{}' 已创建", mc.getBucket());
            }
        } catch (Exception e) {
            log.error("MinIO 初始化失败", e);
        }
        log.info("文件存储使用 MinIO 模式，endpoint: {}", mc.getEndpoint());
    }

    @Override
    public String upload(MultipartFile file, String biz) {
        String safeBiz = sanitizeBiz(biz);
        String ext = extensionFor(file);

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectName = safeBiz + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("MinIO 上传失败: {}", objectName, e);
            throw new BizException(99999, "文件上传失败");
        }

        return properties.getMinio().getEndpoint() + "/" + properties.getMinio().getBucket() + "/" + objectName;
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null) return;
        StorageProperties.Minio mc = properties.getMinio();
        String prefix = mc.getEndpoint() + "/" + mc.getBucket() + "/";
        if (!fileUrl.startsWith(prefix)) return;

        String objectName = fileUrl.substring(prefix.length());
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(mc.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO 删除失败: {}", objectName, e);
        }
    }

    private String sanitizeBiz(String biz) {
        String value = (biz == null || biz.isBlank()) ? "item" : biz.trim();
        if (!value.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new BizException(10001, "业务目录参数不合法");
        }
        return value;
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BizException(10001, "文件类型不能为空");
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new BizException(10001, "不支持的图片类型");
        };
    }
}
