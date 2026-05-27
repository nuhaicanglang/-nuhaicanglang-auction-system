package com.auction.business.job;

import com.auction.business.consumer.EsSyncConsumer;
import com.auction.business.entity.BizAuctionItem;
import com.auction.business.mapper.BizAuctionItemMapper;
import com.auction.search.doc.ItemDoc;
import com.auction.search.repository.ItemDocRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用启动时全量同步 MySQL 商品数据到 ES。
 * 每次启动服务时执行一次，保证 ES 索引与数据库一致。
 * 适合开发/测试环境；生产环境可配置开关或改为手动触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsInitRunner implements ApplicationRunner {

    private final BizAuctionItemMapper itemMapper;
    private final ItemDocRepository itemDocRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("ES 全量同步开始...");
        try {
            // 查询所有未删除的商品（status>=2 表示已通过审核）
            List<BizAuctionItem> items = itemMapper.selectList(
                    new LambdaQueryWrapper<BizAuctionItem>()
                            .ge(BizAuctionItem::getStatus, 2));
            if (items.isEmpty()) {
                log.info("ES 全量同步完成: 无可索引商品");
                return;
            }
            List<ItemDoc> docs = items.stream()
                    .map(EsSyncConsumer::toDoc)
                    .collect(Collectors.toList());
            itemDocRepository.saveAll(docs);
            log.info("ES 全量同步完成: 共索引 {} 条商品", docs.size());
        } catch (Exception e) {
            log.warn("ES 全量同步失败（ES 可能未启动），跳过: {}", e.getMessage());
        }
    }
}
