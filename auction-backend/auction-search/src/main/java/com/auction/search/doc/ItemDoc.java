package com.auction.search.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖商品 ES 文档模型。
 * 索引名 auction_items，mapping 设置分词器和关键字段类型。
 * 用于全文搜索、筛选、排序和聚合。
 */
@Data
@Document(indexName = "auction_items")
@Setting(shards = 1, replicas = 0)
public class ItemDoc {

    @Id
    private Long id;

    /** 标题。开发环境默认 ES 镜像未内置 IK 分词器，先使用标准分词确保索引可创建。 */
    @Field(type = FieldType.Text)
    private String title;

    /** 副标题 */
    @Field(type = FieldType.Text)
    private String subtitle;

    /** 分类ID，用于筛选聚合 */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /** 分类路径，如 "1/5/12" */
    @Field(type = FieldType.Keyword)
    private String categoryPath;

    /** 封面图 */
    @Field(type = FieldType.Keyword, index = false)
    private String coverImage;

    /** 卖家ID */
    @Field(type = FieldType.Long)
    private Long sellerId;

    /** 起拍价 */
    @Field(type = FieldType.Double)
    private BigDecimal startPrice;

    /** 当前价 */
    @Field(type = FieldType.Double)
    private BigDecimal currentPrice;

    /** 一口价 */
    @Field(type = FieldType.Double)
    private BigDecimal buyNowPrice;

    /** 出价次数 */
    @Field(type = FieldType.Integer)
    private Integer bidCount;

    /** 浏览次数 */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /** 状态：2待开/3进行/4已结/5已成/6流拍 */
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 开拍时间 */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime startTime;

    /** 结束时间 */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime endTime;

    /** 创建时间 */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;
}
