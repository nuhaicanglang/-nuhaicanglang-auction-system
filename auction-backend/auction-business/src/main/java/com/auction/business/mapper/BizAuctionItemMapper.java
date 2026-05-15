package com.auction.business.mapper;

import com.auction.business.entity.BizAuctionItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拍卖商品 Mapper。
 */
@Mapper
public interface BizAuctionItemMapper extends BaseMapper<BizAuctionItem> {
}
