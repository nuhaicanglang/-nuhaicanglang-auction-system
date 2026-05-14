package com.auction.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性。
 * 从 application.yml 或 application-dev.yml 的 auction.jwt 节点读取密钥和过期时间。
 */
@Data
@Component
@ConfigurationProperties(prefix = "auction.jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥，HS256 至少需要 32 字节，生产环境应使用环境变量或配置中心管理。
     */
    private String secret;

    /**
     * token 有效期，单位秒。
     */
    private Long expireSeconds;
}
