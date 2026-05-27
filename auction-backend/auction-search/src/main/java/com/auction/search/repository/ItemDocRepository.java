package com.auction.search.repository;

import com.auction.search.doc.ItemDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 拍卖商品 ES Repository。
 * 继承 ElasticsearchRepository 即可获得 CRUD、分页、排序等能力。
 */
public interface ItemDocRepository extends ElasticsearchRepository<ItemDoc, Long> {
}
