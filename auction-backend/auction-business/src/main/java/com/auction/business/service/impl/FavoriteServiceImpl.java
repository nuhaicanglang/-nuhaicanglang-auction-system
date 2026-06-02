package com.auction.business.service.impl;

import com.auction.business.entity.BizAuctionItem;
import com.auction.business.entity.BizFavorite;
import com.auction.business.mapper.BizAuctionItemMapper;
import com.auction.business.mapper.BizFavoriteMapper;
import com.auction.business.service.FavoriteService;
import com.auction.business.vo.AuctionItemVO;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            1, "待审核", 2, "待开拍", 3, "拍卖中", 4, "已结束",
            5, "已成交", 6, "流拍", 7, "已下架");

    private final BizFavoriteMapper favoriteMapper;
    private final BizAuctionItemMapper itemMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    public IPage<AuctionItemVO> listMyFavorites(Long userId, Integer pageNo, Integer pageSize) {
        int page = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);

        Page<BizFavorite> favoritePage = favoriteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<BizFavorite>()
                        .eq(BizFavorite::getUserId, userId)
                        .orderByDesc(BizFavorite::getCreatedAt)
        );
        Page<AuctionItemVO> result = new Page<>(favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());
        if (favoritePage.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        List<Long> itemIds = favoritePage.getRecords().stream().map(BizFavorite::getItemId).toList();
        Map<Long, BizAuctionItem> itemMap = itemMapper.selectBatchIds(itemIds).stream()
                .filter(item -> item.getDeleted() == null || item.getDeleted() == 0)
                .collect(Collectors.toMap(BizAuctionItem::getId, Function.identity()));
        result.setRecords(itemIds.stream()
                .map(itemMap::get)
                .filter(item -> item != null && item.getStatus() != null && item.getStatus() >= 2 && item.getStatus() != 7)
                .map(this::toVO)
                .toList());
        return result;
    }

    @Override
    public boolean isFavorite(Long userId, Long itemId) {
        return favoriteMapper.selectCount(baseRelation(userId, itemId)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long itemId) {
        BizAuctionItem item = getVisibleItem(itemId);
        if (isFavorite(userId, itemId)) {
            return;
        }

        BizFavorite favorite = new BizFavorite();
        favorite.setId(idWorker.nextId());
        favorite.setUserId(userId);
        favorite.setItemId(item.getId());
        favorite.setTenantId(0L);
        favorite.setCreatedAt(LocalDateTime.now());
        try {
            favoriteMapper.insert(favorite);
            itemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BizAuctionItem>()
                    .eq(BizAuctionItem::getId, itemId)
                    .setSql("favorite_count = favorite_count + 1"));
        } catch (DuplicateKeyException ignored) {
            // 并发重复收藏时唯一索引会兜底，业务效果保持幂等。
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long itemId) {
        int deleted = favoriteMapper.delete(baseRelation(userId, itemId));
        if (deleted > 0) {
            itemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BizAuctionItem>()
                    .eq(BizAuctionItem::getId, itemId)
                    .gt(BizAuctionItem::getFavoriteCount, 0)
                    .setSql("favorite_count = favorite_count - 1"));
        }
    }

    private LambdaQueryWrapper<BizFavorite> baseRelation(Long userId, Long itemId) {
        return new LambdaQueryWrapper<BizFavorite>()
                .eq(BizFavorite::getUserId, userId)
                .eq(BizFavorite::getItemId, itemId);
    }

    private BizAuctionItem getVisibleItem(Long itemId) {
        BizAuctionItem item = itemMapper.selectById(itemId);
        if (item == null || (item.getDeleted() != null && item.getDeleted() == 1)
                || item.getStatus() == null || item.getStatus() < 2 || item.getStatus() == 7) {
            throw new BizException(30003, "商品不存在");
        }
        return item;
    }

    private AuctionItemVO toVO(BizAuctionItem item) {
        AuctionItemVO vo = new AuctionItemVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setSubtitle(item.getSubtitle());
        vo.setDescription(item.getDescription());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryPath(item.getCategoryPath());
        vo.setCoverImage(item.getCoverImage());
        vo.setSellerId(item.getSellerId());
        vo.setAuctionType(item.getAuctionType());
        vo.setStartPrice(item.getStartPrice());
        vo.setCurrentPrice(item.getCurrentPrice());
        vo.setBidIncrement(item.getBidIncrement());
        vo.setBuyNowPrice(item.getBuyNowPrice());
        vo.setDeposit(item.getDeposit());
        vo.setStartTime(item.getStartTime());
        vo.setEndTime(item.getEndTime());
        vo.setActualEndTime(item.getActualEndTime());
        vo.setStatus(item.getStatus());
        vo.setStatusText(STATUS_MAP.getOrDefault(item.getStatus(), "未知"));
        vo.setBidCount(item.getBidCount());
        vo.setViewCount(item.getViewCount());
        vo.setFavoriteCount(item.getFavoriteCount());
        vo.setCreatedAt(item.getCreatedAt());
        return vo;
    }
}
