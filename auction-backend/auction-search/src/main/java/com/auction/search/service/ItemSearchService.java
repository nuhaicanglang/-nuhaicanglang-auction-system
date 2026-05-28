package com.auction.search.service;

import com.auction.search.dto.ItemSearchQueryDTO;
import com.auction.search.vo.ItemHitVO;
import com.auction.search.vo.ItemSearchResultVO;

import java.util.List;

/**
 * 商品搜索服务。基于 Elasticsearch 提供全文检索、过滤、高亮、分面聚合等能力。
 */
public interface ItemSearchService {

    /**
     * 搜索商品。
     *
     * @param query  查询条件
     * @param userId 当前用户ID（用于记录搜索历史，可为 null）
     */
    ItemSearchResultVO search(ItemSearchQueryDTO query, Long userId);

    /**
     * 关键词联想（前缀匹配）。
     *
     * @param prefix 关键词前缀
     * @param size   返回数量上限
     */
    List<ItemHitVO> suggest(String prefix, int size);
}
