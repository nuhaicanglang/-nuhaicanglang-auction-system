package com.auction.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * 出价 Lua 脚本配置。
 * 将 bid.lua 加载为 Spring Bean，由 BidService 注入调用。
 * Lua 脚本在 Redis 端原子执行，保证并发出价的正确性。
 */
@Configuration
public class BidScriptConfig {

    @Bean
    public DefaultRedisScript<Long> bidScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/bid.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /** 一口价 Lua 脚本：原子写价格 + 状态 + 入队 */
    @Bean
    public DefaultRedisScript<Long> buyNowScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/buy_now.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
