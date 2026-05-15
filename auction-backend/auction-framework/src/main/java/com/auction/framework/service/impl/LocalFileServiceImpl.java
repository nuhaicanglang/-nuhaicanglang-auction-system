package com.auction.framework.service.impl;

import com.auction.common.exception.BizException;
import com.auction.framework.config.StorageProperties;
import com.auction.framework.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘文件存储实现。
 * 仅在 storage.type=local 时激活（开发环境默认）。
 * <p>
 * 文件按 /{biz}/{yyyy-MM-dd}/{uuid}.{ext} 目录结构存储。
 * 返回的 URL 为 /uploads/{biz}/{date}/{filename}，需 Nginx 或 Spring 静态资源映射。
 * </p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileServiceImpl implements FileService {

    private final StorageProperties properties;

    /** 解析后的绝对路径 */
    private final File baseDir;

    public LocalFileServiceImpl(StorageProperties properties) {
        this.properties = properties;
        // 将配置的相对路径解析为绝对路径（基于 user.dir）
        this.baseDir = new File(properties.getLocalPath()).getAbsoluteFile();
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        log.info("文件存储使用本地磁盘模式，路径: {}", baseDir.getAbsolutePath());
    }

    @Override
    public String upload(MultipartFile file, String biz) {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = biz + "/" + datePath + "/" + fileName;

        File dest = new File(baseDir, relativePath);
        if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
            throw new BizException(99999, "创建上传目录失败");
        }

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件写入失败: {}", dest.getAbsolutePath(), e);
            throw new BizException(99999, "文件上传失败");
        }

        return properties.getLocalUrlPrefix() + "/" + relativePath;
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(properties.getLocalUrlPrefix())) {
            return;
        }
        String relativePath = fileUrl.substring(properties.getLocalUrlPrefix().length() + 1);
        File file = new File(properties.getLocalPath(), relativePath);
        if (file.exists() && !file.delete()) {
            log.warn("删除文件失败: {}", file.getAbsolutePath());
        }
    }
}
