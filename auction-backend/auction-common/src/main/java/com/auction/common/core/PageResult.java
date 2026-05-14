package com.auction.common.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页查询的统一返回对象。
 * 例如商品列表、订单列表、用户列表都可以用这个结构返回。
 *
 * @param <T> 当前页记录的数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页的数据列表。
     */
    private List<T> records;

    /**
     * 符合条件的总记录数。
     */
    private Long total;

    /**
     * 当前页码，从 1 开始。
     */
    private Long pageNum;

    /**
     * 每页大小。
     */
    private Long pageSize;

    /**
     * 构造一个空分页结果，常用于没有查询到数据时返回。
     */
    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
    }

    /**
     * 根据查询结果构造分页对象。
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long pageNum, Long pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize);
    }
}
