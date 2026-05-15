package com.auction.framework.aspect;

import com.auction.common.core.ErrorCode;
import com.auction.common.exception.BizException;
import com.auction.framework.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 幂等切面。
 * 通过 Redis SETNX 保证相同的 X-Idempotent-Key 在指定时间窗口内只执行一次。
 * 如果请求头中没有幂等 key，则不做幂等校验（兼容无需幂等的场景）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String HEADER_KEY = "X-Idempotent-Key";
    private static final String REDIS_PREFIX = "idempotent:";

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotentKey = getIdempotentKey();
        if (!StringUtils.hasText(idempotentKey)) {
            // 未携带幂等 key，直接放行
            return joinPoint.proceed();
        }

        String redisKey = REDIS_PREFIX + idempotentKey;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                redisKey, "1", idempotent.expireSeconds(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 业务异常时删除幂等 key，允许客户端重试
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }

    private String getIdempotentKey() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader(HEADER_KEY);
        }
        return null;
    }
}
