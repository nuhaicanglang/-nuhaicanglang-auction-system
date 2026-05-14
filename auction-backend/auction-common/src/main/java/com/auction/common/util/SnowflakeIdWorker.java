package com.auction.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 雪花算法 ID 生成器。
 * 用于生成全局唯一、趋势递增的 long 类型 ID，适合用户ID、订单ID、出价ID等业务主键。
 */
public class SnowflakeIdWorker {

    /**
     * 自定义起始时间戳：2024-01-01 00:00:00。
     */
    private static final long EPOCH = 1704067200000L;

    /**
     * 机器ID占 5 位，最多支持 32 台机器。
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 机房ID占 5 位，最多支持 32 个机房。
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 序列号占 12 位，同一毫秒内最多生成 4096 个 ID。
     */
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 默认构造方法。
     * 开发环境下随机分配 workerId，单机运行足够使用。
     */
    public SnowflakeIdWorker() {
        this(ThreadLocalRandom.current().nextLong(MAX_WORKER_ID + 1), 0L);
    }

    /**
     * 指定机器ID和机房ID，生产环境多实例部署时建议显式配置。
     */
    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个唯一ID。
     * synchronized 用来保证同一个生成器实例在并发调用时不会产生重复序列号。
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards");
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒内序列号用完时，等待进入下一毫秒。
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 阻塞等待到下一毫秒。
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前系统时间，单独封装便于后续测试或扩展。
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
