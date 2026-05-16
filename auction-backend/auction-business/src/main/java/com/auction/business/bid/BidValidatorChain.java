package com.auction.business.bid;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 出价校验链组装器。
 * <p>
 * Spring 启动时自动收集容器内全部 {@link AbstractBidValidator} Bean，
 * 按 {@code order()} 升序串成单链，对外暴露唯一入口 {@link #execute(BidContext)}。
 * </p>
 * <p>新增校验器只需 {@code @Component} 一个新子类，无需修改链本身——开闭原则。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidValidatorChain {

    private final List<AbstractBidValidator> validators;
    private AbstractBidValidator head;

    @PostConstruct
    public void init() {
        // 按 order 升序排序
        List<AbstractBidValidator> sorted = validators.stream()
                .sorted(Comparator.comparingInt(AbstractBidValidator::order))
                .toList();

        if (sorted.isEmpty()) {
            return;
        }
        head = sorted.get(0);
        for (int i = 0; i < sorted.size() - 1; i++) {
            sorted.get(i).setNext(sorted.get(i + 1));
        }

        log.info("出价校验链组装完成，共 {} 个节点：{}", sorted.size(),
                sorted.stream().map(v -> v.getClass().getSimpleName()).toList());
    }

    /** 执行校验链，任一节点抛 BizException 即中断。 */
    public void execute(BidContext ctx) {
        if (head != null) {
            head.validate(ctx);
        }
    }
}
