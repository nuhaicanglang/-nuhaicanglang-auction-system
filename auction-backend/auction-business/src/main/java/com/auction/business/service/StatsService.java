package com.auction.business.service;

import com.auction.business.vo.CategoryHotVO;
import com.auction.business.vo.ItemTopVO;
import com.auction.business.vo.StatsOverviewVO;
import com.auction.business.vo.StatsTrendPointVO;

import java.util.List;

/**
 * 管理仪表盘统计服务。
 */
public interface StatsService {

    /** 概览：今日 + 累计 + 进行中。 */
    StatsOverviewVO overview();

    /** 趋势：过去 N 天的新增商品/订单/成交额（含今日）。 */
    List<StatsTrendPointVO> trend(int days);

    /** 热门分类 TOP N。 */
    List<CategoryHotVO> hotCategories(int limit);

    /** 热门商品 TOP N。 */
    List<ItemTopVO> topItems(int limit);
}
