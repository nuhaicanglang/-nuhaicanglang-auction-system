package com.auction.business.service.impl;

import com.auction.business.dto.AuctionItemCreateDTO;
import com.auction.business.dto.AuctionItemQueryDTO;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.entity.BizCategory;
import com.auction.business.mapper.BizAuctionItemMapper;
import com.auction.business.mapper.BizCategoryMapper;
import com.auction.business.service.AuctionItemService;
import com.auction.business.vo.AuctionItemVO;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.ItemSyncMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 拍卖商品服务实现。
 * <p>
 * 核心逻辑：
 * 1. 发布：校验分类 → 填充冗余字段 → 初始 status=1(待审)
 * 2. 编辑：仅 status=1(待审) 或 auditStatus=2(驳回) 时可改
 * 3. 下架：仅卖家可下架自己的商品
 * 4. 列表：支持分类/状态/价格/关键字筛选 + 排序 + 分页
 * 5. 详情：增加浏览数，返回卖家信息
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionItemServiceImpl extends ServiceImpl<BizAuctionItemMapper, BizAuctionItem>
        implements AuctionItemService {

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            1, "待审核", 2, "待开拍", 3, "拍卖中", 4, "已结束",
            5, "已成交", 6, "流拍", 7, "已下架");

    private final BizCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    public Long publishItem(AuctionItemCreateDTO dto, Long sellerId) {
        // 校验分类
        BizCategory category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BizException(30001, "分类不存在");
        }

        BizAuctionItem item = new BizAuctionItem();
        item.setId(idWorker.nextId());
        item.setTitle(dto.getTitle());
        item.setSubtitle(dto.getSubtitle());
        item.setDescription(dto.getDescription());
        item.setCategoryId(dto.getCategoryId());
        item.setCategoryPath(category.getPath());
        item.setCoverImage(dto.getCoverImage());
        item.setImages(toJson(dto.getImages()));
        item.setSellerId(sellerId);

        item.setAuctionType(1);
        item.setStartPrice(dto.getStartPrice());
        item.setCurrentPrice(dto.getStartPrice());
        item.setBidIncrement(dto.getBidIncrement());
        item.setBuyNowPrice(dto.getBuyNowPrice());
        item.setDeposit(dto.getDeposit() != null ? dto.getDeposit() : java.math.BigDecimal.ZERO);

        // 计算开始/结束时间
        LocalDateTime startTime;
        if ("SCHEDULED".equalsIgnoreCase(dto.getStartMode()) && dto.getScheduledStartTime() != null) {
            startTime = dto.getScheduledStartTime();
        } else {
            // IMMEDIATE 模式：审核通过后才真正开拍，这里先设为当前时间+1天（占位）
            startTime = LocalDateTime.now().plusDays(1);
        }
        item.setStartTime(startTime);
        item.setEndTime(startTime.plusMinutes(dto.getDuration()));

        item.setStatus(1);      // 待审核
        item.setAuditStatus(0); // 待审
        item.setIsAntiSnipe(1);
        item.setAntiSnipeMin(5);
        item.setTenantId(0L);
        item.setCreatedBy(sellerId);
        item.setBidCount(0);
        item.setViewCount(0);
        item.setFavoriteCount(0);

        save(item);
        return item.getId();
    }

    @Override
    public void updateItem(Long id, AuctionItemCreateDTO dto, Long sellerId) {
        BizAuctionItem item = getById(id);
        if (item == null) {
            throw new BizException(30003, "商品不存在");
        }
        if (!item.getSellerId().equals(sellerId)) {
            throw new BizException(30004, "只能编辑自己的商品");
        }
        // 仅待审核(status=1) 或审核驳回(auditStatus=2) 状态可编辑
        if (item.getStatus() != 1 && item.getAuditStatus() != 2) {
            throw new BizException(30004, "当前状态不允许编辑");
        }

        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(item.getCategoryId())) {
            BizCategory category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                throw new BizException(30001, "分类不存在");
            }
            item.setCategoryId(dto.getCategoryId());
            item.setCategoryPath(category.getPath());
        }

        item.setTitle(dto.getTitle());
        item.setSubtitle(dto.getSubtitle());
        item.setDescription(dto.getDescription());
        item.setCoverImage(dto.getCoverImage());
        item.setImages(toJson(dto.getImages()));
        item.setStartPrice(dto.getStartPrice());
        item.setCurrentPrice(dto.getStartPrice());
        item.setBidIncrement(dto.getBidIncrement());
        item.setBuyNowPrice(dto.getBuyNowPrice());
        if (dto.getDeposit() != null) {
            item.setDeposit(dto.getDeposit());
        }
        // 重新进入待审核
        item.setStatus(1);
        item.setAuditStatus(0);
        item.setUpdatedBy(sellerId);

        updateById(item);
    }

    @Override
    public void offlineItem(Long id, Long sellerId) {
        BizAuctionItem item = getById(id);
        if (item == null) {
            throw new BizException(30003, "商品不存在");
        }
        if (!item.getSellerId().equals(sellerId)) {
            throw new BizException(30004, "只能下架自己的商品");
        }
        // 仅待审(1)、待开(2) 可主动下架
        if (item.getStatus() != 1 && item.getStatus() != 2) {
            throw new BizException(30004, "当前状态不允许下架");
        }
        item.setStatus(7);
        item.setUpdatedBy(sellerId);
        updateById(item);
        sendItemSync(id, "DELETE");
    }

    /** 发送商品 ES 同步消息 */
    public void sendItemSync(Long itemId, String action) {
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_DIRECT, MqConstants.RK_ITEM_SYNC,
                ItemSyncMessage.builder().itemId(itemId).action(action).build());
    }

    @Override
    public IPage<AuctionItemVO> listItems(AuctionItemQueryDTO query) {
        LambdaQueryWrapper<BizAuctionItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizAuctionItem::getDeleted, 0);

        if (query.getCategoryId() != null) {
            wrapper.eq(BizAuctionItem::getCategoryId, query.getCategoryId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BizAuctionItem::getStatus, query.getStatus());
        }
        if (query.getPriceMin() != null) {
            wrapper.ge(BizAuctionItem::getCurrentPrice, query.getPriceMin());
        }
        if (query.getPriceMax() != null) {
            wrapper.le(BizAuctionItem::getCurrentPrice, query.getPriceMax());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(BizAuctionItem::getTitle, query.getKeyword());
        }
        if (query.getSellerId() != null) {
            wrapper.eq(BizAuctionItem::getSellerId, query.getSellerId());
        }

        // 排序：默认按创建时间倒序
        if (StringUtils.hasText(query.getSort())) {
            String sortField = query.getSort();
            boolean asc = true;
            if (sortField.startsWith("-")) {
                asc = false;
                sortField = sortField.substring(1);
            }
            switch (sortField) {
                case "end_time" -> wrapper.orderBy(true, asc, BizAuctionItem::getEndTime);
                case "current_price" -> wrapper.orderBy(true, asc, BizAuctionItem::getCurrentPrice);
                case "bid_count" -> wrapper.orderBy(true, asc, BizAuctionItem::getBidCount);
                default -> wrapper.orderByDesc(BizAuctionItem::getCreatedAt);
            }
        } else {
            wrapper.orderByDesc(BizAuctionItem::getCreatedAt);
        }

        Page<BizAuctionItem> page = new Page<>(query.getPage(), query.getSize());
        Page<BizAuctionItem> result = page(page, wrapper);

        return result.convert(this::toVO);
    }

    @Override
    public AuctionItemVO getItemDetail(Long id) {
        BizAuctionItem item = getById(id);
        if (item == null) {
            throw new BizException(30003, "商品不存在");
        }

        // 浏览数 +1
        lambdaUpdate().eq(BizAuctionItem::getId, id)
                .setSql("view_count = view_count + 1")
                .update();

        return toVO(item);
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
        vo.setImages(fromJson(item.getImages()));
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

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
