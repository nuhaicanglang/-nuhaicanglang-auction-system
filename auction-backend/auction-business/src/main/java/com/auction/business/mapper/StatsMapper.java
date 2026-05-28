package com.auction.business.mapper;

import com.auction.business.vo.CategoryHotVO;
import com.auction.business.vo.ItemTopVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计 Mapper：聚合查询不依赖单一实体，使用注解 SQL。
 */
@Mapper
public interface StatsMapper {

    /** 统计指定时间范围内新增用户数（sys_user 未删除） */
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0 "
            + "AND created_at >= #{start} AND created_at < #{end}")
    long countUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 累计用户 */
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    long countUsersTotal();

    /** 统计指定时间范围内新增商品（biz_auction_item 未删除） */
    @Select("SELECT COUNT(*) FROM biz_auction_item WHERE deleted = 0 "
            + "AND created_at >= #{start} AND created_at < #{end}")
    long countItemsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM biz_auction_item WHERE deleted = 0")
    long countItemsTotal();

    /** 进行中拍卖（status=3） */
    @Select("SELECT COUNT(*) FROM biz_auction_item WHERE deleted = 0 AND status = 3")
    long countOngoingAuctions();

    /** 统计指定时间范围内新增订单 */
    @Select("SELECT COUNT(*) FROM biz_order WHERE created_at >= #{start} AND created_at < #{end}")
    long countOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM biz_order")
    long countOrdersTotal();

    /** 待支付订单（status=1） */
    @Select("SELECT COUNT(*) FROM biz_order WHERE status = 1")
    long countPendingPayments();

    /**
     * 指定时间范围内成交金额：以 biz_payment 实际支付成功流水（status=1）为准。
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM biz_payment "
            + "WHERE status = 1 AND paid_at >= #{start} AND paid_at < #{end}")
    BigDecimal sumDealAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM biz_payment WHERE status = 1")
    BigDecimal sumDealAmountTotal();

    /**
     * 趋势：按日聚合新增商品。
     * 返回 {date(yyyy-MM-dd), count}。
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS count "
            + "FROM biz_auction_item WHERE deleted = 0 "
            + "AND created_at >= #{start} AND created_at < #{end} "
            + "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')")
    List<Map<String, Object>> trendItemDaily(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS count "
            + "FROM biz_order "
            + "WHERE created_at >= #{start} AND created_at < #{end} "
            + "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')")
    List<Map<String, Object>> trendOrderDaily(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Select("SELECT DATE_FORMAT(paid_at, '%Y-%m-%d') AS date, COALESCE(SUM(amount), 0) AS amount "
            + "FROM biz_payment "
            + "WHERE status = 1 AND paid_at >= #{start} AND paid_at < #{end} "
            + "GROUP BY DATE_FORMAT(paid_at, '%Y-%m-%d')")
    List<Map<String, Object>> trendDealDaily(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    /**
     * 热门分类 TOP N：按拍卖中/已成交商品数。
     */
    @Select("SELECT i.category_id AS categoryId, c.name AS categoryName, COUNT(*) AS itemCount "
            + "FROM biz_auction_item i LEFT JOIN biz_category c ON c.id = i.category_id "
            + "WHERE i.deleted = 0 AND i.status IN (3, 5) "
            + "GROUP BY i.category_id, c.name "
            + "ORDER BY itemCount DESC LIMIT #{limit}")
    List<CategoryHotVO> hotCategories(@Param("limit") int limit);

    /**
     * 热门商品 TOP N：按出价数 + 浏览数综合。
     */
    @Select("SELECT id AS itemId, title, category_id AS categoryId, current_price AS currentPrice, "
            + "final_price AS finalPrice, bid_count AS bidCount, view_count AS viewCount, status "
            + "FROM biz_auction_item "
            + "WHERE deleted = 0 AND status >= 2 "
            + "ORDER BY bid_count DESC, view_count DESC LIMIT #{limit}")
    List<ItemTopVO> topItems(@Param("limit") int limit);
}
