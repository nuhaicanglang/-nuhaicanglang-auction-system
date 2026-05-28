package com.auction.search.service.impl;

import com.auction.framework.redis.RedisKey;
import com.auction.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 搜索历史 Redis 实现：
 * <ul>
 *   <li>使用 List 结构，按 LPUSH 顺序存储</li>
 *   <li>同关键词去重（先 LREM 再 LPUSH）</li>
 *   <li>限长 {@link #MAX_HISTORY}（LTRIM）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addHistory(Long userId, String keyword) {
        if (userId == null || !StringUtils.hasText(keyword)) {
            return;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 50) {
            trimmed = trimmed.substring(0, 50);
        }
        String key = RedisKey.searchHistory(userId);
        // 去重：删除已存在的相同关键词
        redisTemplate.opsForList().remove(key, 0, trimmed);
        // 推入队首
        redisTemplate.opsForList().leftPush(key, trimmed);
        // 限长
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY - 1L);
    }

    @Override
    public List<String> getHistory(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> list = redisTemplate.opsForList()
                .range(RedisKey.searchHistory(userId), 0, MAX_HISTORY - 1L);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void clearHistory(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(RedisKey.searchHistory(userId));
    }
}
