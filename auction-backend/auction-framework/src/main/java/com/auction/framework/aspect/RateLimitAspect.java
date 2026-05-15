package com.auction.framework.aspect;

import com.auction.common.core.ErrorCode;
import com.auction.common.exception.BizException;
import com.auction.framework.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

/**
 * 限流切面。
 * 基于 Redis Lua 脚本进行原子计数，超限抛出 SYSTEM_LIMITED 异常。
 * 限流 key 按 注解key + 客户端IP 组合，实现每 IP 独立限流。
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        this.rateLimitScript.setResultType(Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String ip = getClientIp();
        String key = "rate_limit:" + rateLimit.key() + ":" + ip;

        Long allowed = redisTemplate.execute(rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(rateLimit.count()),
                String.valueOf(rateLimit.period()));

        if (allowed == null || allowed == 0) {
            throw new BizException(ErrorCode.SYSTEM_LIMITED);
        }
        return joinPoint.proceed();
    }

    private String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}
