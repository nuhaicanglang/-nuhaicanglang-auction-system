package com.auction.framework.config;

import com.auction.framework.websocket.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 配置。
 * <p>
 * STOMP（Simple Text Oriented Messaging Protocol）是 WebSocket 上的一层子协议：
 * <ul>
 *   <li>前端通过 stompjs 连接，统一按"目的地（destination）"订阅/发送消息。</li>
 *   <li>后端用 {@link org.springframework.messaging.simp.SimpMessagingTemplate} 推送。</li>
 * </ul>
 * 当前使用 Spring 内置的 SimpleBroker（内存），后续可替换为 RabbitMQ STOMP Relay。
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    /**
     * 注册 STOMP 端点：前端通过 ws://host/api/ws?token=xxx 建立连接。
     * allowedOriginPatterns("*") 开发阶段允许所有来源，生产环境应收窄。
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 兼容不支持原生 WebSocket 的旧浏览器
    }

    /**
     * 配置消息代理。
     * <ul>
     *   <li>/topic  → 广播（一对多），如商品出价更新。</li>
     *   <li>/queue  → 点对点（一对一），如个人通知。</li>
     *   <li>/app    → 前端向后端发送消息的前缀（经过 @MessageMapping 处理）。</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单内存代理，处理 /topic 和 /queue 前缀的订阅
        registry.enableSimpleBroker("/topic", "/queue");
        // 前端向服务端发消息时，destination 需以 /app 开头
        registry.setApplicationDestinationPrefixes("/app");
        // 点对点消息前缀（用于 convertAndSendToUser）
        registry.setUserDestinationPrefix("/user");
    }
}
