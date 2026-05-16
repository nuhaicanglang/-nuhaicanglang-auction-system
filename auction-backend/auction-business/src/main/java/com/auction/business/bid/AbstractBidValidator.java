package com.auction.business.bid;

/**
 * 出价校验器抽象基类。
 * <p>
 * 设计模式：
 * <ul>
 *   <li><b>责任链</b>：每个校验器持有 next 引用，逐节执行；</li>
 *   <li><b>模板方法</b>：父类定义骨架 {@link #validate(BidContext)}，
 *       子类只需实现 {@link #doValidate(BidContext)} 关注本节点的校验逻辑，
 *       是否继续传递由父类统一控制。</li>
 * </ul>
 * 任一节点抛出 BizException → 中断链，由全局异常处理器响应。
 * </p>
 */
public abstract class AbstractBidValidator {

    /** 下一个校验节点 */
    private AbstractBidValidator next;

    public AbstractBidValidator setNext(AbstractBidValidator next) {
        this.next = next;
        return next;
    }

    /**
     * 模板方法：执行本节点校验，再驱动下一节点。
     * 子类不要覆盖此方法，覆盖 {@link #doValidate(BidContext)} 即可。
     */
    public final void validate(BidContext ctx) {
        doValidate(ctx);
        if (next != null) {
            next.validate(ctx);
        }
    }

    /**
     * 子类实现：本节点的校验逻辑。
     * 校验失败请抛 {@link com.auction.common.exception.BizException}。
     */
    protected abstract void doValidate(BidContext ctx);

    /**
     * 排序值，越小越靠前。Spring 自动收集时根据该值组装成链。
     */
    public abstract int order();
}
