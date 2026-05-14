package com.auction.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 开发阶段的临时安全配置。
 * 当前先放行所有接口，等登录和权限模块完成后再替换成真正的认证授权规则。
 */
@Configuration
public class SecurityPermitAllConfig {

    /**
     * 构建 Spring Security 过滤链。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * 密码加密器。
     * BCrypt 会自动加盐，同一个明文密码每次加密后的结果也不同，更适合保存用户密码。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
