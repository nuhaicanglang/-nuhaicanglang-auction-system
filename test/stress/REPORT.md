# Day 15 压测报告

## 1. 测试环境

| 项目 | 配置 |
|------|------|
| OS | Windows 11 |
| JDK | 17 |
| Spring Boot | 3.x |
| MySQL | 8.0 (Docker) |
| Redis | 7.x (Docker) |
| 并发工具 | PowerShell 5.1 + .NET HttpClient Task.WhenAll |

## 2. 调优参数（application-dev.yml）

```yaml
server.tomcat.threads.max: 200
server.tomcat.threads.min-spare: 20
server.tomcat.accept-count: 100
spring.datasource.hikari.maximum-pool-size: 30
spring.datasource.hikari.minimum-idle: 10
spring.data.redis.lettuce.pool.max-active: 32
spring.data.redis.lettuce.pool.max-idle: 16
spring.data.redis.lettuce.pool.min-idle: 8
```

新增 `commons-pool2` 依赖以启用 Lettuce 连接池。

## 3. 测试场景

- 100 个独立 bidder 账号同时对同一商品出价
- 每人出价金额递增（110, 120, ..., 1100），bid_increment=10
- 使用 HttpClient.SendAsync 异步并发，Task.WaitAll 等待全部完成

## 4. 测试结果

### Round 1（冷启动后首轮）

| 指标 | 值 |
|------|-----|
| 总请求 | 100 |
| 成功 | 3 |
| 失败 | 97 |
| 耗时 | 0.8s |
| QPS | 125.4 |
| AVG | 671.6ms |
| P50 | 651ms |
| P95 | 775ms |
| P99 | 794ms |
| 失败原因 | 100% "出价金额不足" |

### Round 2（热状态）

| 指标 | 值 |
|------|-----|
| 总请求 | 100 |
| 成功 | 1 |
| 失败 | 99 |
| 耗时 | 0.35s |
| QPS | 287.4 |
| AVG | 218.8ms |
| P50 | 203ms |
| P95 | 336ms |
| P99 | 338ms |
| 失败原因 | 100% "出价金额不足" |

## 5. 验证结论

| 验收项 | 结果 |
|--------|------|
| 100 并发无报错 | ✅ PASS |
| 失败仅"出价不足" | ✅ PASS |
| Redis 最高价 = MySQL 最高价 = 最高成功出价 | ✅ PASS |
| biz_bid 行数 = 成功出价数 | ✅ PASS |
| 无重复出价 | ✅ PASS（幂等 key 保证） |
| QPS > 100 | ✅ PASS（冷启动 125，热态 287） |
| P99 < 1s | ✅ PASS（794ms / 338ms） |

## 6. 说明

- 成功数少（1~3）是预期行为：100 个并发请求几乎同时到达 Redis Lua 脚本，
  Lua 串行执行，只有出价高于 `当前价 + bid_increment` 的才会成功。
  由于出价价格递增，只有最高的几个在 Lua 执行顺序中恰好领先才能成功。
- 这恰恰证明了 **Redis Lua 原子出价的正确性**：并发安全、价格单调递增、无超卖。
- WebSocket 推送和反狙击延时已在 Day 14 手动验证通过。
