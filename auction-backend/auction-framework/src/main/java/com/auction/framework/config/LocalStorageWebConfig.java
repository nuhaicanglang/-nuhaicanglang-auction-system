package com.auction.framework.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 本地存储静态资源映射。
 * 仅在 storage.type=local 时生效。
 * 将 /uploads/** 映射到本地磁盘目录，方便开发环境直接通过 URL 访问上传的文件。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String localPath = new File(storageProperties.getLocalPath()).getAbsolutePath();
        registry.addResourceHandler(storageProperties.getLocalUrlPrefix() + "/**")
                .addResourceLocations("file:" + localPath + "/");
    }
}
