package com.auction.framework.security;

import com.auction.framework.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 令牌工具。
 * 负责生成、解析和校验登录 token，是认证过滤器和登录服务共用的安全组件。
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 创建登录 token（带角色列表）。
     * subject 存用户ID，username 和 roles 放在自定义声明中，
     * 这样每次请求解析 token 就能知道当前用户的角色，无需再查数据库。
     */
    /** Refresh Token 有效期：7 天 */
    private static final long REFRESH_EXPIRE_SECONDS = 7 * 24 * 3600;

    /**
     * 创建 Access Token（带角色列表）。
     * 包含 jti（唯一ID）和 type=access 标识，用于黑名单和区分 token 类型。
     */
    public String createToken(Long userId, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 创建 Refresh Token，只携带 userId，有效期 7 天。
     */
    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(REFRESH_EXPIRE_SECONDS);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 token 中的声明数据。
     * 如果 token 被篡改或已过期，JJWT 会抛出异常，由认证过滤器统一处理。
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 token 的过期时间。
     */
    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * 根据配置密钥生成 HMAC 签名 Key。
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
