package com.auction.business.mapper;

import com.auction.business.entity.BizPayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水 Mapper。
 */
@Mapper
public interface BizPaymentMapper extends BaseMapper<BizPayment> {
}
