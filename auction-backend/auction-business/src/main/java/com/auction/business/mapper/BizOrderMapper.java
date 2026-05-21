package com.auction.business.mapper;

import com.auction.business.entity.BizOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper。
 */
@Mapper
public interface BizOrderMapper extends BaseMapper<BizOrder> {
}
