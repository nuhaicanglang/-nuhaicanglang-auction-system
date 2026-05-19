package com.auction.framework.websocket;

import com.auction.framework.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手拦截器。
 * <p>
 * 由于浏览器 WebSocket API 不能自定义 Header，
 * 因此前端通过 URL Query 携带 token：ws://host/api/ws?token=xxx
 * 本拦截器在握手阶段解析该 token，把用户信息写入 attributes，
 * 后续 STOMP 帧和控制器可通过 attributes 获取当前用户。
 * </p>
 * 未携带 token 或 token 无效时 → 允许匿名连接（订阅公开主题），
 * 如需强制鉴权可在此返回 false 拒绝握手。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // 从 URL 的 Query 参数解析 token
        List<String> tokenList = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams()
                .get("token");

        String token = (tokenList != null && !tokenList.isEmpty()) ? tokenList.get(0) : null;

        if (token != null && !token.isBlank()) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);

                // 写入 WebSocket session attributes，供后续使用
                attributes.put("userId", userId);
                attributes.put("username", username);

                log.debug("WebSocket 握手成功，userId={}, username={}", userId, username);
            } catch (Exception e) {
                // token 解析失败 → 允许匿名连接，不写 userId/username
                log.warn("WebSocket 握手 token 解析失败，将以匿名连接：{}", e.getMessage());
            }
        }

        // 返回 true：允许建立连接（公开主题无需登录）
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后无额外处理
    }
}
