package com.auction.framework.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口。
 * 提供统一的上传/删除方法，具体由 MinIO 或本地磁盘实现。
 */
public interface FileService {

    /**
     * 上传文件。
     *
     * @param file 上传的文件
     * @param biz  业务标识，用于路径分类（如 item / avatar）
     * @return 文件访问 URL
     */
    String upload(MultipartFile file, String biz);

    /**
     * 删除文件。
     *
     * @param fileUrl 文件 URL
     */
    void delete(String fileUrl);
}
