package com.auction.framework.redis;

/**
 * Redis Key 统一管理。
 * 所有业务 Key 格式在此定义，避免散落各处导致冲突或拼写错误。
 */
public final class RedisKey {

    private RedisKey() {}

    /** 商品当前价格：auction:price:{itemId} */
    public static String itemPrice(Long itemId) {
        return "auction:price:" + itemId;
    }

    /** 商品当前状态：auction:status:{itemId} */
    public static String itemStatus(Long itemId) {
        return "auction:status:" + itemId;
    }

    /** 出价队列（Lua 脚本 LPUSH）：auction:bid:queue:{itemId} */
    public static String bidQueue(Long itemId) {
        return "auction:bid:queue:" + itemId;
    }

    /** 出价幂等锁：auction:idem:{requestId} */
    public static String idem(String requestId) {
        return "auction:idem:" + requestId;
    }

    /** 商品信息缓存：auction:item:{itemId} */
    public static String itemInfo(Long itemId) {
        return "auction:item:" + itemId;
    }

    /** 分类树缓存 */
    public static final String CATEGORY_TREE = "cache:category:tree";

    public static String creditDaily(Long userId, String date) {
        return "credit:daily:" + userId + ":" + date;
    }

    /** 用户搜索历史：search:history:{userId} */
    public static String searchHistory(Long userId) {
        return "search:history:" + userId;
    }
}
