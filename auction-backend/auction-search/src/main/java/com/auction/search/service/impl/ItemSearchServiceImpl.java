package com.auction.search.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.auction.search.doc.ItemDoc;
import com.auction.search.dto.ItemSearchQueryDTO;
import com.auction.search.service.ItemSearchService;
import com.auction.search.service.SearchHistoryService;
import com.auction.search.vo.FacetVO;
import com.auction.search.vo.ItemHitVO;
import com.auction.search.vo.ItemSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品搜索服务实现，基于 Elasticsearch Java Client 8.x + Spring Data ES NativeQuery。
 * <ul>
 *   <li>bool 查询：keyword 走 multi_match (title^3 + subtitle)；categoryId/status/priceRange 走 filter</li>
 *   <li>高亮：title、subtitle 字段，&lt;em&gt; 标签</li>
 *   <li>分面聚合：分类、状态 terms 聚合 + 价格 range 聚合</li>
 *   <li>排序：相关度 / 结束时间 / 当前价 / 出价数 / 创建时间</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSearchServiceImpl implements ItemSearchService {

    private static final String AGG_CATEGORY = "by_category";
    private static final String AGG_STATUS = "by_status";
    private static final String AGG_PRICE = "by_price";

    private final ElasticsearchOperations esOps;
    private final SearchHistoryService searchHistoryService;

    @Override
    public ItemSearchResultVO search(ItemSearchQueryDTO query, Long userId) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 50);

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(buildQuery(query))
                .withPageable(PageRequest.of(page - 1, size))
                .withSort(buildSort(query))
                .withAggregation(AGG_CATEGORY, Aggregation.of(a -> a
                        .terms(t -> t.field("categoryId").size(20))))
                .withAggregation(AGG_STATUS, Aggregation.of(a -> a
                        .terms(t -> t.field("status").size(10))))
                .withAggregation(AGG_PRICE, Aggregation.of(a -> a
                        .range(r -> r.field("currentPrice")
                                .ranges(priceRanges()))));

        if (StringUtils.hasText(query.getKeyword())) {
            builder.withHighlightQuery(buildHighlight());
        }

        SearchHits<ItemDoc> hits;
        try {
            hits = esOps.search(builder.build(), ItemDoc.class);
        } catch (Exception e) {
            log.warn("ES 搜索失败：{}", e.getMessage());
            ItemSearchResultVO empty = new ItemSearchResultVO();
            empty.setTotal(0L);
            empty.setPage(page);
            empty.setSize(size);
            empty.setItems(Collections.emptyList());
            empty.setCategoryFacets(Collections.emptyList());
            empty.setStatusFacets(Collections.emptyList());
            empty.setPriceFacets(Collections.emptyList());
            return empty;
        }

        ItemSearchResultVO vo = new ItemSearchResultVO();
        vo.setTotal(hits.getTotalHits());
        vo.setPage(page);
        vo.setSize(size);
        vo.setItems(hits.stream().map(this::toHit).collect(Collectors.toList()));
        vo.setCategoryFacets(parseTermsLong(hits, AGG_CATEGORY));
        vo.setStatusFacets(parseTermsLong(hits, AGG_STATUS));
        vo.setPriceFacets(parseRange(hits, AGG_PRICE));

        // 搜索历史（仅有关键词且登录用户且开启 saveHistory 时记录）
        if (Boolean.TRUE.equals(query.getSaveHistory())
                && userId != null
                && StringUtils.hasText(query.getKeyword())) {
            try {
                searchHistoryService.addHistory(userId, query.getKeyword());
            } catch (Exception e) {
                log.warn("保存搜索历史失败：{}", e.getMessage());
            }
        }
        return vo;
    }

    @Override
    public List<ItemHitVO> suggest(String prefix, int size) {
        if (!StringUtils.hasText(prefix)) {
            return Collections.emptyList();
        }
        int sz = size <= 0 ? 10 : Math.min(size, 20);
        Query q = Query.of(b -> b.bool(bb -> bb
                .must(m -> m.multiMatch(mm -> mm
                        .query(prefix)
                        .fields("title^3", "subtitle")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.PhrasePrefix)))
                .filter(f -> f.terms(t -> t
                        .field("status")
                        .terms(tv -> tv.value(Arrays.asList(
                                FieldValue.of(2L), FieldValue.of(3L))))))));

        NativeQuery nq = NativeQuery.builder()
                .withQuery(q)
                .withPageable(PageRequest.of(0, sz))
                .build();
        try {
            SearchHits<ItemDoc> hits = esOps.search(nq, ItemDoc.class);
            return hits.stream().map(this::toHit).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("ES suggest 失败：{}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /* ============== 私有工具 ============== */

    private Query buildQuery(ItemSearchQueryDTO query) {
        return Query.of(q -> q.bool(b -> {
            // must：关键词
            if (StringUtils.hasText(query.getKeyword())) {
                b.must(m -> m.multiMatch(mm -> mm
                        .query(query.getKeyword().trim())
                        .fields("title^3", "subtitle")));
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }
            // filter：分类
            if (query.getCategoryId() != null) {
                b.filter(f -> f.term(t -> t
                        .field("categoryId")
                        .value(query.getCategoryId())));
            }
            // filter：状态（指定则精确匹配，未指定则只显示 status>=2 的可见商品）
            if (query.getStatus() != null) {
                b.filter(f -> f.term(t -> t
                        .field("status")
                        .value(query.getStatus())));
            } else {
                b.filter(f -> f.range(r -> r
                        .field("status")
                        .gte(JsonData.of(2))));
            }
            // filter：价格区间
            BigDecimal min = query.getMinPrice();
            BigDecimal max = query.getMaxPrice();
            if (min != null || max != null) {
                b.filter(f -> f.range(r -> {
                    r.field("currentPrice");
                    if (min != null) r.gte(JsonData.of(min.doubleValue()));
                    if (max != null) r.lte(JsonData.of(max.doubleValue()));
                    return r;
                }));
            }
            return b;
        }));
    }

    private List<SortOptions> buildSort(ItemSearchQueryDTO query) {
        String sort = query.getSort();
        if (!StringUtils.hasText(sort) || "relevance".equalsIgnoreCase(sort)) {
            return Collections.singletonList(SortOptions.of(s -> s
                    .score(sc -> sc.order(SortOrder.Desc))));
        }
        SortOrder order = "asc".equalsIgnoreCase(query.getOrder()) ? SortOrder.Asc : SortOrder.Desc;
        String field;
        switch (sort.toLowerCase()) {
            case "endtime":      field = "endTime"; break;
            case "currentprice": field = "currentPrice"; break;
            case "bidcount":     field = "bidCount"; break;
            case "createdat":    field = "createdAt"; break;
            default:             field = "createdAt"; break;
        }
        final String f = field;
        return Collections.singletonList(SortOptions.of(s -> s
                .field(fld -> fld.field(f).order(order))));
    }

    private HighlightQuery buildHighlight() {
        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .withRequireFieldMatch(false)
                .build();
        HighlightFieldParameters fieldParams = HighlightFieldParameters.builder()
                .withNumberOfFragments(0)
                .build();
        Highlight highlight = new Highlight(params, Arrays.asList(
                new HighlightField("title", fieldParams),
                new HighlightField("subtitle", fieldParams)));
        return new HighlightQuery(highlight, ItemDoc.class);
    }

    private List<AggregationRange> priceRanges() {
        return Arrays.asList(
                AggregationRange.of(r -> r.key("0-100").to("100")),
                AggregationRange.of(r -> r.key("100-500").from("100").to("500")),
                AggregationRange.of(r -> r.key("500-1000").from("500").to("1000")),
                AggregationRange.of(r -> r.key("1000-5000").from("1000").to("5000")),
                AggregationRange.of(r -> r.key("5000+").from("5000")));
    }

    private ItemHitVO toHit(SearchHit<ItemDoc> hit) {
        ItemDoc doc = hit.getContent();
        ItemHitVO vo = new ItemHitVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setSubtitle(doc.getSubtitle());
        vo.setCategoryId(doc.getCategoryId());
        vo.setCoverImage(doc.getCoverImage());
        vo.setSellerId(doc.getSellerId());
        vo.setCurrentPrice(doc.getCurrentPrice());
        vo.setBuyNowPrice(doc.getBuyNowPrice());
        vo.setBidCount(doc.getBidCount());
        vo.setStatus(doc.getStatus());
        vo.setStartTime(doc.getStartTime());
        vo.setEndTime(doc.getEndTime());

        List<String> hl = hit.getHighlightField("title");
        vo.setHighlightTitle(hl != null && !hl.isEmpty() ? String.join("", hl) : doc.getTitle());
        List<String> hl2 = hit.getHighlightField("subtitle");
        vo.setHighlightSubtitle(hl2 != null && !hl2.isEmpty() ? String.join("", hl2) : doc.getSubtitle());
        return vo;
    }

    private List<FacetVO> parseTermsLong(SearchHits<?> hits, String name) {
        Aggregate agg = getAggregate(hits, name);
        if (agg == null) return Collections.emptyList();
        if (agg.isLterms()) {
            return agg.lterms().buckets().array().stream()
                    .map(b -> new FacetVO(String.valueOf(b.key()), b.docCount()))
                    .collect(Collectors.toList());
        }
        if (agg.isSterms()) {
            return agg.sterms().buckets().array().stream()
                    .map(b -> new FacetVO(b.key().stringValue(), b.docCount()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<FacetVO> parseRange(SearchHits<?> hits, String name) {
        Aggregate agg = getAggregate(hits, name);
        if (agg == null || !agg.isRange()) return Collections.emptyList();
        return agg.range().buckets().array().stream()
                .map(b -> new FacetVO(b.key(), b.docCount()))
                .collect(Collectors.toList());
    }

    private Aggregate getAggregate(SearchHits<?> hits, String name) {
        if (hits.getAggregations() == null) return null;
        ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
        for (ElasticsearchAggregation ea : aggs.aggregations()) {
            if (name.equals(ea.aggregation().getName())) {
                return ea.aggregation().getAggregate();
            }
        }
        return null;
    }
}
