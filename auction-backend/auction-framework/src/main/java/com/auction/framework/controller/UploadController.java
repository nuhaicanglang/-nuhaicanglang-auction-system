package com.auction.framework.controller;

import com.auction.common.core.Result;
import com.auction.framework.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new com.auction.common.exception.BizException(40001, "单个文件不能超过 5MB");
        }
    }
}
