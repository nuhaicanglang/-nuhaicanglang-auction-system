package com.auction.search.service;

import java.util.List;

/**
 * 搜索历史服务。
 * 使用 Redis List 存储每个用户最近的搜索关键词，去重、限长。
 */
public interface SearchHistoryService {

    /** 历史记录最大条数 */
    int MAX_HISTORY = 20;

    /** 添加一条搜索历史（已存在则前置） */
    void addHistory(Long userId, String keyword);

    /** 获取当前用户搜索历史（最近在前） */
    List<String> getHistory(Long userId);

    /** 清空当前用户搜索历史 */
    void clearHistory(Long userId);
}
