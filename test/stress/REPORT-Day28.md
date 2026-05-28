# Day 28 全面压测报告

> 本报告用于记录 Day 28 出价、列表、搜索三个核心场景的压测方法与结果。当前提交先补齐可重复执行的压测脚本和报告模板，实际重压测需在 MySQL、Redis、RabbitMQ、Elasticsearch、后端服务全部启动后手动执行。

## 1. 测试环境

| 项目 | 配置 |
|---|---|
| OS | Windows 11 |
| JDK | 17 |
| Spring Boot | 3.2.5 |
| MySQL | 8.0 Docker |
| Redis | 7.x Docker |
| RabbitMQ | Docker |
| Elasticsearch | 8.x Docker |
| 并发工具 | PowerShell + .NET HttpClient Task.WhenAll |
| 后端地址 | `http://localhost:8080` |

## 2. 前置准备

### 2.1 启动依赖

```powershell
docker ps
```

需要确认：

- MySQL 已启动
- Redis 已启动
- RabbitMQ 已启动
- Elasticsearch 已启动
- 后端服务已启动并能访问 `/api/ping`

### 2.2 准备出价压测账号和商品

```powershell
# 在 MySQL 容器内执行，或使用数据库客户端执行
docker exec -i auction-mysql mysql -uroot -proot123456 auction < test/stress/setup.sql
```

该脚本会：

- 基于 `bidder01` 的密码哈希创建 `bidder02~bidder100`
- 为压测用户绑定 USER 角色
- 重置测试商品状态、价格、结束时间
- 清空该商品已有出价记录

### 2.3 ES 索引准备

Day 28 搜索压测依赖 Day 24/25 的 ES 索引与搜索接口。

如果搜索结果为空，先确认：

- Elasticsearch 已启动
- 后端启动日志中 `EsInitRunner` 全量同步执行过
- 商品审核通过后已发送 `ITEM_SYNC` 消息

## 3. 压测脚本

### 3.1 出价场景

脚本：`test/stress/stress-bid.ps1`

```powershell
powershell -ExecutionPolicy Bypass -File test/stress/stress-bid.ps1 `
  -BaseUrl "http://localhost:8080" `
  -ItemId "313631694601076736" `
  -UserCount 100 `
  -Password "Test123456" `
  -StartBid 110 `
  -Increment 10
```

关注点：

- Redis Lua 原子出价是否并发安全
- QPS、P50、P95、P99
- Redis 最高价、MySQL 当前价、最高成功出价是否一致
- `biz_bid` 行数是否等于成功出价数
- 失败原因是否集中在“出价不足”等预期业务失败

### 3.2 商品列表场景

脚本：`test/stress/stress-list.ps1`

```powershell
powershell -ExecutionPolicy Bypass -File test/stress/stress-list.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Concurrent 100 `
  -TotalRequests 1000 `
  -PageSize 10
```

脚本会并发请求：

```http
GET /api/items?page={1~10}&size=10&status={2|3}&sort={createdAt|currentPrice}
```

关注点：

- 列表接口 QPS
- 分页查询 P95/P99
- 是否存在慢 SQL
- 分类/状态/排序组合是否稳定
- MySQL 连接池是否出现耗尽

### 3.3 搜索场景

脚本：`test/stress/stress-search.ps1`

```powershell
powershell -ExecutionPolicy Bypass -File test/stress/stress-search.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Concurrent 100 `
  -TotalRequests 1000 `
  -PageSize 10 `
  -Keywords 国画,瓷器,书法,玉器,邮票
```

脚本会混合请求：

```http
GET /api/search/items?keyword=国画&page=1&size=10&status=3&sort=relevance
GET /api/search/suggest?prefix=国画&size=10
```

关注点：

- ES 搜索接口 QPS
- ES 查询 P95/P99
- 高亮和聚合是否正常返回
- ES 不可用时接口是否优雅降级为空结果
- 后端线程池/ES client 是否稳定

## 4. 结果记录模板

### 4.1 出价场景结果

| 指标 | 值 |
|---|---|
| 总请求 | 待填写 |
| 成功 | 待填写 |
| 失败 | 待填写 |
| 总耗时 | 待填写 |
| QPS | 待填写 |
| AVG | 待填写 |
| P50 | 待填写 |
| P95 | 待填写 |
| P99 | 待填写 |
| Redis 最高价 | 待填写 |
| MySQL 当前价 | 待填写 |
| 最高成功出价 | 待填写 |
| `biz_bid` 行数 | 待填写 |

### 4.2 列表场景结果

| 指标 | 值 |
|---|---|
| 总请求 | 待填写 |
| 成功 | 待填写 |
| 失败 | 待填写 |
| 总耗时 | 待填写 |
| QPS | 待填写 |
| AVG | 待填写 |
| P50 | 待填写 |
| P95 | 待填写 |
| P99 | 待填写 |

### 4.3 搜索场景结果

| 指标 | 值 |
|---|---|
| 总请求 | 待填写 |
| 成功 | 待填写 |
| 失败 | 待填写 |
| 总耗时 | 待填写 |
| QPS | 待填写 |
| AVG | 待填写 |
| P50 | 待填写 |
| P95 | 待填写 |
| P99 | 待填写 |

## 5. 验收标准

| 场景 | 验收项 | 目标 |
|---|---|---|
| 出价 | 并发安全 | Redis 最高价 = MySQL 当前价 = 最高成功出价 |
| 出价 | 数据一致 | `biz_bid` 行数 = 成功出价数 |
| 出价 | 失败可解释 | 失败主要为“出价不足”等业务失败 |
| 列表 | 接口稳定 | 1000 请求无 5xx |
| 列表 | 延迟 | P99 建议 < 1000ms |
| 搜索 | 接口稳定 | 1000 请求无 5xx |
| 搜索 | 延迟 | P99 建议 < 1500ms |
| 搜索 | 结果正确 | 高亮、聚合字段正常返回 |

## 6. 常见问题

| 问题 | 原因 | 处理 |
|---|---|---|
| 登录失败 | bidder 账号未准备 | 执行 `setup.sql` |
| 出价全部失败 | 商品状态/价格未重置 | 执行 `setup.sql` 并确认 status=3 |
| 列表 QPS 很低 | MySQL 慢查询/连接池不足 | 查看 SQL 日志和 Hikari 指标 |
| 搜索全部为空 | ES 未启动或未同步 | 启动 ES 并检查 `EsInitRunner` 日志 |
| 搜索接口 5xx | ES 连接异常或 mapping 不兼容 | 查看后端日志与 ES 容器日志 |
| PowerShell 执行被阻止 | 执行策略限制 | 使用 `-ExecutionPolicy Bypass` |

## 7. 结论

当前已补齐 Day 28 三类压测脚本：

- `stress-bid.ps1`：出价并发压测（沿用 Day 15）
- `stress-list.ps1`：商品列表压测
- `stress-search.ps1`：ES 搜索/联想压测

实际压测结果需在完整中间件和后端服务启动后执行脚本并回填本报告。
