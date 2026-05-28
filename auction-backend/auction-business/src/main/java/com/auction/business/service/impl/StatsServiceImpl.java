package com.auction.business.service.impl;

import com.auction.business.mapper.StatsMapper;
import com.auction.business.service.StatsService;
import com.auction.business.vo.CategoryHotVO;
import com.auction.business.vo.ItemTopVO;
import com.auction.business.vo.StatsOverviewVO;
import com.auction.business.vo.StatsTrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现。
 * 使用一组 SQL 聚合查询组合出概览、趋势、热门分类、热门商品。
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;

    @Override
    public StatsOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTodayNewUsers(statsMapper.countUsersBetween(start, end));
        vo.setTodayNewItems(statsMapper.countItemsBetween(start, end));
        vo.setTodayNewOrders(statsMapper.countOrdersBetween(start, end));
        vo.setTodayDealAmount(zeroIfNull(statsMapper.sumDealAmountBetween(start, end)));

        vo.setTotalUsers(statsMapper.countUsersTotal());
        vo.setTotalItems(statsMapper.countItemsTotal());
        vo.setTotalOrders(statsMapper.countOrdersTotal());
        vo.setTotalDealAmount(zeroIfNull(statsMapper.sumDealAmountTotal()));

        vo.setOngoingAuctions(statsMapper.countOngoingAuctions());
        vo.setPendingPayments(statsMapper.countPendingPayments());
        return vo;
    }

    @Override
    public List<StatsTrendPointVO> trend(int days) {
        int n = days <= 0 ? 30 : Math.min(days, 90);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(n - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Map<String, Long> itemMap = toCountMap(statsMapper.trendItemDaily(start, end));
        Map<String, Long> orderMap = toCountMap(statsMapper.trendOrderDaily(start, end));
        Map<String, BigDecimal> dealMap = toAmountMap(statsMapper.trendDealDaily(start, end));

        List<StatsTrendPointVO> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String day = startDate.plusDays(i).toString();
            result.add(new StatsTrendPointVO(
                    day,
                    itemMap.getOrDefault(day, 0L),
                    orderMap.getOrDefault(day, 0L),
                    dealMap.getOrDefault(day, BigDecimal.ZERO)));
        }
        return result;
    }

    @Override
    public List<CategoryHotVO> hotCategories(int limit) {
        return statsMapper.hotCategories(normalizeLimit(limit));
    }

    @Override
    public List<ItemTopVO> topItems(int limit) {
        return statsMapper.topItems(normalizeLimit(limit));
    }

    /* ============== 工具 ============== */

    private int normalizeLimit(int limit) {
        if (limit <= 0) return 10;
        return Math.min(limit, 50);
    }

    private BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> r : rows) {
            String date = (String) r.get("date");
            Object cnt = r.get("count");
            if (date != null && cnt != null) {
                map.put(date, ((Number) cnt).longValue());
            }
        }
        return map;
    }

    private Map<String, BigDecimal> toAmountMap(List<Map<String, Object>> rows) {
        Map<String, BigDecimal> map = new HashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> r : rows) {
            String date = (String) r.get("date");
            Object amt = r.get("amount");
            if (date != null && amt != null) {
                map.put(date, amt instanceof BigDecimal ? (BigDecimal) amt : new BigDecimal(amt.toString()));
            }
        }
        return map;
    }

    @SuppressWarnings("unused")
    private LocalDateTime startOfDay(LocalDate d) { return d.atTime(LocalTime.MIN); }
}
