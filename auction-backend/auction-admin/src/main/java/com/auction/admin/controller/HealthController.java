package com.auction.admin.controller;

import com.auction.common.core.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查接口。
 * 用于快速确认后端服务是否已经启动，以及统一返回格式和 traceId 是否生效。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 最简单的连通性测试接口。
     */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        return Result.success(Map.of(
                "message", "auction backend is running",
                "time", LocalDateTime.now().toString()
        ));
    }
}
