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
import com.auction.framework.redis.RedisKey;
import com.auction.mq.constant.MqConstants;
import com.auction.mq.message.AuctionSettleMessage;
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
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
    private final StringRedisTemplate redisTemplate;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    private static final String SAMPLE_AUDIT_REMARK = "管理员批量注入样例商品";
    private static final List<SampleItemSpec> SAMPLE_ITEMS = List.of(
            new SampleItemSpec("溪山清远图", "设色山水长卷，适合书房陈列", "<p>仿古设色山水，层峦叠翠，适合中式空间陈设。</p>", 111L,
                    "/sample-items/art-landscape.svg", List.of("/sample-items/art-landscape.svg"),
                    new BigDecimal("680.00"), new BigDecimal("30.00"), new BigDecimal("980.00"), new BigDecimal("60.00"), 360),
            new SampleItemSpec("墨韵行书条幅", "行书作品，装裱完成可直接悬挂", "<p>纸本行书，笔意流畅，适合作为办公室或茶室陈设。</p>", 113L,
                    "/sample-items/art-calligraphy.svg", List.of("/sample-items/art-calligraphy.svg"),
                    new BigDecimal("520.00"), new BigDecimal("20.00"), new BigDecimal("760.00"), new BigDecimal("50.00"), 480),
            new SampleItemSpec("铜鎏金人物雕塑", "桌面陈列雕塑，细节完整", "<p>铜鎏金工艺雕塑，适合客厅边柜、展厅和收藏展示。</p>", 120L,
                    "/sample-items/sculpture-bronze.svg", List.of("/sample-items/sculpture-bronze.svg"),
                    new BigDecimal("1280.00"), new BigDecimal("50.00"), new BigDecimal("1880.00"), new BigDecimal("120.00"), 720),
            new SampleItemSpec("经典旁轴胶片相机", "带原装皮套，镜头通透", "<p>收藏向胶片相机，机身成色优秀，附带皮套与肩带。</p>", 330L,
                    "/sample-items/camera-vintage.svg", List.of("/sample-items/camera-vintage.svg"),
                    new BigDecimal("1680.00"), new BigDecimal("60.00"), new BigDecimal("2360.00"), new BigDecimal("150.00"), 600),
            new SampleItemSpec("Aurora X1 旗舰手机", "12GB + 512GB，成色近新", "<p>高端旗舰手机，屏幕与机身状态良好，适合数码爱好者竞拍。</p>", 310L,
                    "/sample-items/smartphone-aurora.svg", List.of("/sample-items/smartphone-aurora.svg"),
                    new BigDecimal("2399.00"), new BigDecimal("80.00"), new BigDecimal("3299.00"), new BigDecimal("200.00"), 540),
            new SampleItemSpec("Carbon Pro 轻薄本", "商务超轻薄，附原装充电器", "<p>14 英寸轻薄本，适合办公与差旅使用，外观保持良好。</p>", 320L,
                    "/sample-items/laptop-carbon.svg", List.of("/sample-items/laptop-carbon.svg"),
                    new BigDecimal("3299.00"), new BigDecimal("100.00"), new BigDecimal("4399.00"), new BigDecimal("300.00"), 840),
            new SampleItemSpec("Chronos 机械腕表", "蓝钢指针，背透机芯", "<p>经典机械腕表，表盘干净，适合作为日常佩戴与收藏单品。</p>", 400L,
                    "/sample-items/watch-chrono.svg", List.of("/sample-items/watch-chrono.svg"),
                    new BigDecimal("2860.00"), new BigDecimal("120.00"), new BigDecimal("3980.00"), new BigDecimal("300.00"), 900),
            new SampleItemSpec("和田玉圆条手镯", "温润细腻，附检测证书", "<p>手镯玉质温润，适合珠宝拍卖场景展示与陈列。</p>", 500L,
                    "/sample-items/jade-bracelet.svg", List.of("/sample-items/jade-bracelet.svg"),
                    new BigDecimal("1880.00"), new BigDecimal("80.00"), new BigDecimal("2680.00"), new BigDecimal("180.00"), 720),
            new SampleItemSpec("黑胶唱片机套装", "胡桃木纹机身，含试听唱片", "<p>复古黑胶唱机，适合生活方式类拍品展示与家居搭配。</p>", 200L,
                    "/sample-items/vinyl-record.svg", List.of("/sample-items/vinyl-record.svg"),
                    new BigDecimal("980.00"), new BigDecimal("40.00"), new BigDecimal("1480.00"), new BigDecimal("80.00"), 660),
            new SampleItemSpec("限量版钢笔礼盒", "树脂笔杆，礼盒齐全", "<p>商务礼赠向钢笔礼盒，适合作为文房与收藏类商品补充。</p>", 910L,
                    "/sample-items/fountain-pen.svg", List.of("/sample-items/fountain-pen.svg"),
                    new BigDecimal("460.00"), new BigDecimal("20.00"), new BigDecimal("720.00"), new BigDecimal("30.00"), 420)
    );

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
        } else if (query.getSellerId() == null) {
            wrapper.in(BizAuctionItem::getStatus, 2, 3, 4, 5, 6);
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
        if (item.getDeleted() != null && item.getDeleted() == 1
                || item.getStatus() == null
                || item.getStatus() < 2
                || item.getStatus() == 7) {
            throw new BizException(30003, "商品不存在");
        }

        // 浏览数 +1
        lambdaUpdate().eq(BizAuctionItem::getId, id)
                .setSql("view_count = view_count + 1")
                .update();

        return toVO(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createSampleItems(Integer count, Long operatorId) {
        int safeCount = count == null ? 12 : Math.max(1, Math.min(count, 30));
        LocalDateTime now = LocalDateTime.now();
        List<Long> itemIds = new ArrayList<>(safeCount);

        for (int i = 0; i < safeCount; i++) {
            SampleItemSpec spec = SAMPLE_ITEMS.get(i % SAMPLE_ITEMS.size());
            BizCategory category = categoryMapper.selectById(spec.categoryId());
            if (category == null) {
                throw new BizException(30001, "样例商品分类不存在: " + spec.categoryId());
            }

            BizAuctionItem item = new BizAuctionItem();
            item.setId(idWorker.nextId());
            item.setTitle(spec.title() + " · 样例" + String.format("%02d", i + 1));
            item.setSubtitle(spec.subtitle());
            item.setDescription(spec.description());
            item.setCategoryId(spec.categoryId());
            item.setCategoryPath(category.getPath());
            item.setCoverImage(spec.coverImage());
            item.setImages(toJson(spec.images()));
            item.setSellerId(operatorId);
            item.setAuctionType(1);
            item.setStartPrice(spec.startPrice());
            item.setCurrentPrice(spec.startPrice());
            item.setBidIncrement(spec.bidIncrement());
            item.setBuyNowPrice(spec.buyNowPrice());
            item.setDeposit(spec.deposit());

            LocalDateTime startTime = now.minusMinutes(15L + i * 4L);
            LocalDateTime endTime = now.plusMinutes((long) spec.durationMinutes() + i * 6L);
            item.setStartTime(startTime);
            item.setEndTime(endTime);

            item.setStatus(3);
            item.setAuditStatus(1);
            item.setAuditRemark(SAMPLE_AUDIT_REMARK);
            item.setAuditBy(operatorId);
            item.setAuditAt(now);
            item.setBidCount(i % 5);
            item.setViewCount(28 + i * 11);
            item.setFavoriteCount(i % 7);
            item.setIsAntiSnipe(1);
            item.setAntiSnipeMin(5);
            item.setTenantId(0L);
            item.setCreatedBy(operatorId);
            item.setUpdatedBy(operatorId);

            save(item);
            publishLiveSample(item);
            itemIds.add(item.getId());
        }

        return itemIds;
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

    private void publishLiveSample(BizAuctionItem item) {
        redisTemplate.opsForValue().set(RedisKey.itemPrice(item.getId()), item.getCurrentPrice().toPlainString());

        long ttlMs = Duration.between(LocalDateTime.now(), item.getEndTime()).toMillis();
        if (ttlMs < 1000) {
            ttlMs = 1000;
        }
        AuctionSettleMessage settleMessage = AuctionSettleMessage.builder()
                .itemId(item.getId())
                .expectedEndTimeMs(item.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();
        String expiration = String.valueOf(ttlMs);
        MessagePostProcessor ttlSetter = message -> {
            message.getMessageProperties().setExpiration(expiration);
            return message;
        };
        rabbitTemplate.convertAndSend(
                MqConstants.EXCHANGE_DIRECT,
                MqConstants.RK_AUCTION_DELAY,
                settleMessage,
                ttlSetter
        );
        sendItemSync(item.getId(), "UPSERT");
    }

    private record SampleItemSpec(
            String title,
            String subtitle,
            String description,
            Long categoryId,
            String coverImage,
            List<String> images,
            BigDecimal startPrice,
            BigDecimal bidIncrement,
            BigDecimal buyNowPrice,
            BigDecimal deposit,
            Integer durationMinutes
    ) {
    }
}
