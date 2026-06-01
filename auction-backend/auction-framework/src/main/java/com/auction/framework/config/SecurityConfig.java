package com.auction.framework.config;

import com.auction.common.constant.TraceConstants;
import com.auction.common.core.ErrorCode;
import com.auction.common.core.Result;
import com.auction.framework.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 安全配置。
 * 当前阶段先完成 JWT 登录态校验，后续 RBAC 权限码会继续在这里扩展。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    private final ObjectMapper objectMapper;

    /**
     * 构建 Spring Security 过滤链：登录注册放行，其它接口必须携带有效 token。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/ping",
                                "/api/system/users/register",
                                "/api/system/users/login",
                                "/api/system/users/captcha",
                                "/api/system/users/refresh",
                                "/api/categories/**",
                                "/uploads/**",
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/actuator/health",
                                "/api/ws/**"   // WebSocket/SockJS 握手端点，鉴权由 JwtHandshakeInterceptor 完成
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/*", "/api/items/*/bids").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/items", "/api/search/suggest").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, ErrorCode.FORBIDDEN)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 开发联调用跨域配置，允许 Vite 前端携带 Authorization 等请求头访问后端 API。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 密码加密器。
     * BCrypt 会自动加盐，同一个明文密码每次加密后的结果也不同，更适合保存用户密码。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security 异常发生在 Controller 之前，不能走全局异常处理器，所以这里手动输出统一 Result。
     */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> result = Result.<Void>fail(errorCode).withTraceId(MDC.get(TraceConstants.TRACE_ID));
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
