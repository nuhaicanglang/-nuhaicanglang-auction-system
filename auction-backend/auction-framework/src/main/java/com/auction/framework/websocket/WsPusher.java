package com.auction.framework.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * WebSocket 推送工具。
 * <p>
 * 对 {@link SimpMessagingTemplate} 做一层封装，统一管理推送主题的格式和日志。
 * 其他 Service（如 BidServiceImpl）直接注入本类即可，不需要关心底层发送细节。
 * </p>
 *
 * 三条主题（与文档 06-关键技术方案 约定一致）：
 * <ul>
 *   <li>{@code /topic/auction/{itemId}}         → 出价更新广播</li>
 *   <li>{@code /topic/auction/{itemId}/state}   → 拍卖状态变化广播</li>
 *   <li>{@code /user/{username}/queue/notification} → 个人通知（点对点）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsPusher {

    private final SimpMessagingTemplate messagingTemplate;

    // ----------------------------------------------------------------
    // 消息体记录（用 Map 构造 JSON，保持轻量，不引入额外 VO 类）
    // ----------------------------------------------------------------

    /**
     * 出价成功后广播给所有订阅该商品的客户端。
     *
     * @param itemId       商品ID
     * @param bidderId     出价人ID
     * @param bidderName   出价人脱敏昵称
     * @param newPrice     新的当前价
     * @param bidId        本次出价记录ID
     */
    public void pushBidPlaced(Long itemId, Long bidderId,
                              String bidderName, BigDecimal newPrice, Long bidId) {
        String destination = "/topic/auction/" + itemId;
        Map<String, Object> payload = Map.of(
                "type", "BID_PLACED",
                "itemId", itemId,
                "bidderId", bidderId,
                "bidderName", bidderName,
                "currentPrice", newPrice,
                "bidId", bidId,
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("WS推送出价: dest={}, price={}, bidder={}", destination, newPrice, bidderName);
    }

    /**
     * 拍卖状态变化广播（开拍、流拍、成交等）。
     *
     * @param itemId    商品ID
     * @param newStatus 新状态码（3=拍卖中，4=已结，5=已成交，6=流拍，7=下架）
     * @param message   可读描述
     */
    public void pushAuctionStateChange(Long itemId, int newStatus, String message) {
        String destination = "/topic/auction/" + itemId + "/state";
        Map<String, Object> payload = Map.of(
                "type", "STATE_CHANGE",
                "itemId", itemId,
                "newStatus", newStatus,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("WS推送状态变化: dest={}, status={}", destination, newStatus);
    }

    /**
     * 向特定用户发送个人通知（如被超价通知）。
     *
     * @param username 目标用户的 username（Principal name）
     * @param title    通知标题
     * @param content  通知内容
     */
    public void pushUserNotification(String username, String title, String content) {
        Map<String, Object> payload = Map.of(
                "type", "NOTIFICATION",
                "title", title,
                "content", content,
                "timestamp", System.currentTimeMillis()
        );
        // convertAndSendToUser 会自动拼接为 /user/{username}/queue/notification
        messagingTemplate.convertAndSendToUser(username, "/queue/notification", payload);
        log.debug("WS个人通知: user={}, title={}", username, title);
    }
}
