package com.auction.business.service;

import com.auction.business.vo.AuctionItemVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface FavoriteService {

    IPage<AuctionItemVO> listMyFavorites(Long userId, Integer page, Integer size);

    boolean isFavorite(Long userId, Long itemId);

    void addFavorite(Long userId, Long itemId);

    void removeFavorite(Long userId, Long itemId);
}
