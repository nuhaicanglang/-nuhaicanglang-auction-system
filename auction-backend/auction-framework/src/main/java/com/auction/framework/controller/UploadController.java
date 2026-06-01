package com.auction.framework.controller;

import com.auction.common.core.Result;
import com.auction.framework.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传控制器。
 * <p>
 * 支持单张和批量上传，返回文件访问 URL。
 * biz 参数指定业务类型（如 item/avatar），用于文件路径分类。
 * 上传接口需登录（Security 已拦截），大小限制由 Spring 配置控制。
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class UploadController {

    private final FileService fileService;

    /**
     * 上传单张图片。
     */
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "biz", defaultValue = "item") String biz) {
        validateImage(file);
        String url = fileService.upload(file, biz);
        return Result.success(url);
    }

    /**
     * 批量上传图片。
     */
    @PostMapping("/multi")
    public Result<List<String>> uploadMulti(@RequestParam("files") MultipartFile[] files,
                                            @RequestParam(value = "biz", defaultValue = "item") String biz) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            validateImage(file);
            urls.add(fileService.upload(file, biz));
        }
        return Result.success(urls);
    }

    /**
     * 校验图片文件：非空、类型白名单、大小限制 5MB。
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.auction.common.exception.BizException(40001, "文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new com.auction.common.exception.BizException(40001, "仅支持图片文件");
        }
        if (!hasValidImageHeader(file)) {
            throw new com.auction.common.exception.BizException(40001, "图片文件内容不合法");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new com.auction.common.exception.BizException(40001, "单个文件不能超过 5MB");
        }
    }

    private boolean hasValidImageHeader(MultipartFile file) {
        byte[] header = new byte[12];
        try (InputStream input = file.getInputStream()) {
            int len = input.read(header);
            if (len < 4) {
                return false;
            }
            boolean jpg = (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8
                    && (header[2] & 0xff) == 0xff;
            boolean png = len >= 8
                    && (header[0] & 0xff) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4e
                    && header[3] == 0x47
                    && header[4] == 0x0d
                    && header[5] == 0x0a
                    && header[6] == 0x1a
                    && header[7] == 0x0a;
            boolean gif = header[0] == 0x47
                    && header[1] == 0x49
                    && header[2] == 0x46
                    && header[3] == 0x38;
            boolean webp = len >= 12
                    && header[0] == 0x52
                    && header[1] == 0x49
                    && header[2] == 0x46
                    && header[3] == 0x46
                    && header[8] == 0x57
                    && header[9] == 0x45
                    && header[10] == 0x42
                    && header[11] == 0x50;
            return jpg || png || gif || webp;
        } catch (IOException e) {
            return false;
        }
    }
}
