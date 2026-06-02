package com.auction.business.service;

import com.auction.business.dto.AuctionItemCreateDTO;
import com.auction.business.dto.AuctionItemQueryDTO;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.vo.AuctionItemVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 拍卖商品服务接口。
 */
public interface AuctionItemService extends IService<BizAuctionItem> {

    /** 发布商品（卖家） */
    Long publishItem(AuctionItemCreateDTO dto, Long sellerId);

    /** 编辑商品（仅待审/驳回状态可改） */
    void updateItem(Long id, AuctionItemCreateDTO dto, Long sellerId);

    /** 下架自己的商品 */
    void offlineItem(Long id, Long sellerId);

    /** 分页查询商品列表 */
    IPage<AuctionItemVO> listItems(AuctionItemQueryDTO query);

    /** 商品详情 */
    AuctionItemVO getItemDetail(Long id);

    /** 管理员批量创建带图样例商品 */
    List<Long> createSampleItems(Integer count, Long operatorId);
}
