# 在线拍卖系统 Online Auction System

> Java Web 高级开发技术大作业。当前以后端为主，围绕用户权限、商品管理、高并发出价、Redis Lua 原子竞价、后续 WebSocket/MQ/订单结算逐步实现。

## 项目状态

当前已完成到 **阶段 3 / Day 15：压测与调优**。

| 阶段 | 主题 | 状态 | 主要产出 |
|---|---|---|---|
| Day 0~5 | 工程骨架、用户认证、RBAC、AOP | 已完成 | 多模块 Maven、JWT、Spring Security、角色权限、操作日志、限流、幂等 |
| Day 6 | 分类树 | 已完成 | `biz_category`、分类树、Redis 缓存、管理端 CRUD |
| Day 7 | 文件上传 | 已完成 | 本地上传、MinIO 条件配置、静态资源映射 |
| Day 8~9 | 商品发布、查询、审核 | 已完成 | `biz_auction_item`、发布/编辑/下架、分页查询、详情、审核/强制下架 |
| Day 10 | Redis 预热与 Lua 脚本 | 已完成 | `RedisKey`、`bid.lua`、`BidScriptConfig`、启动预热拍卖中商品价格 |
| Day 11 | 出价主流程 | 已完成 | `BidController`、`BidServiceImpl`、Redis Lua 原子出价、出价记录 |
| Day 12 | 出价校验链 | 已完成 | 责任链 + 模板方法：参数、商品状态、不能给自己出价、频率、价格校验 |
| Day 13 | WebSocket 实时推送 | 已完成 | `WebSocketConfig` STOMP/SockJS、`JwtHandshakeInterceptor` URL token 鉴权、`WsPusher` 三主题推送、出价后广播 |
| Day 14 | 反狙击延时 + 一口价 | 已完成 | 出价临近结束自动延时、`buy_now.lua` 一口价原子脚本、`/buy-now` 接口、成交状态广播 |
| Day 15 | 压测与调优 | 已完成 | 100并发出价压测脚本、Tomcat/HikariCP/Lettuce调优、commons-pool2、压测报告 |

## 技术栈

- **后端框架**：Spring Boot 3.2.5、Spring MVC、Spring Validation
- **安全认证**：Spring Security、JWT、BCrypt、`@PreAuthorize`
- **ORM**：MyBatis-Plus、分页插件、乐观锁插件
- **数据库**：MySQL 8，初始化脚本位于 `deploy/mysql/init.sql`
- **缓存/并发**：Redis、Lua 脚本、SETNX 限流/幂等
- **文件存储**：本地上传，MinIO 条件启用
- **接口文档**：Knife4j / OpenAPI
- **后续模块**：WebSocket、RabbitMQ、订单/保证金、ES 搜索、压测

## 功能模块总览

### 1. 用户与权限

- **注册/登录**：支持用户注册、登录、刷新 Token、登出。
- **JWT 鉴权**：使用 Access Token + Refresh Token，登出后 Token 加入 Redis 黑名单。
- **RBAC 权限模型**：用户、角色、权限三层模型。
- **管理端权限控制**：管理端接口使用 `@PreAuthorize` 控制角色访问。
- **登录安全**：图形验证码、登录失败计数、账号锁定。
- **操作日志**：`@Log` 注解 + AOP 异步记录操作日志。
- **通用横切能力**：已实现 `@RateLimit` 限流、`@Idempotent` 幂等注解。

### 2. 商品分类

- **分类树**：公开接口返回树形分类数据。
- **Redis 缓存**：分类树高频读取走缓存。
- **管理端 CRUD**：新增、编辑、删除、启停分类。
- **分类路径冗余**：商品保存 `categoryPath`，便于后续按层级筛选。

### 3. 文件上传

- **本地存储**：开发环境默认保存到 `./uploads`。
- **MinIO 适配**：保留 MinIO 条件加载实现，后续可切换对象存储。
- **静态资源映射**：本地上传文件可通过 `/uploads/**` 访问。
- **大小限制**：单文件 5MB，总请求 30MB。

### 4. 拍卖商品

- **发布商品**：卖家登录后发布，初始状态为待审核。
- **编辑商品**：限制仅待审核/驳回等安全状态可编辑。
- **下架商品**：卖家可下架自己的商品。
- **列表查询**：支持分页、分类、状态、价格区间、关键词、排序。
- **详情查询**：返回商品完整 VO，并增加浏览次数。
- **管理员审核**：审核通过后进入待开拍，管理员可强制下架。

### 5. 出价核心

- **Redis 预热**：应用启动扫描拍卖中商品，将当前价写入 Redis。
- **Lua 原子出价**：在 Redis 端完成幂等、价格比较、更新当前价、入队。
- **出价记录**：成功出价写入 `biz_bid`，可查询商品出价记录。
- **出价校验链**：责任链 + 模板方法实现参数、状态、身份、频率、价格校验。
- **频率限制**：同一用户同一商品 1 秒内只能出价一次。
- **WebSocket 推送**：出价成功后广播 `/topic/auction/{itemId}`，状态变化广播 `/topic/auction/{itemId}/state`。
- **反狙击延时**：拍卖临近结束时出价，自动延长 `end_time` 并推送状态变化。
- **一口价成交**：支持 `POST /api/items/{itemId}/bids/buy-now`，直接以 `buy_now_price` 成交并写入状态 `5=已成交`。

## 目录结构

```text
大作业/
├── README.md
├── docs/                         # 需求、架构、数据库、接口、关键方案、里程碑
├── deploy/                       # Docker 中间件与 MySQL/Redis 配置
│   ├── docker-compose.middleware.yml
│   ├── mysql/
│   └── redis/
└── auction-backend/              # Spring Boot 多模块后端
    ├── auction-admin/            # 启动入口
    ├── auction-common/           # 通用返回、异常、工具
    ├── auction-framework/        # Security、Redis、AOP、上传等框架能力
    ├── auction-system/           # 用户、角色、权限
    ├── auction-business/         # 分类、商品、出价等核心业务
    ├── auction-mq/               # MQ 模块预留
    ├── auction-search/           # 搜索模块预留
    └── auction-job/              # 定时任务模块预留
```

## 快速启动

> 所有命令以 Windows PowerShell 为例。

### 1. 启动中间件

```powershell
docker compose -f deploy/docker-compose.middleware.yml up -d
```

包含：

- **MySQL**：`localhost:3306`，数据库 `auction`，root 密码 `root123456`
- **Redis**：`localhost:6379`
- **当前 compose**：主要启动 MySQL + Redis，RabbitMQ / ES / MinIO 后续阶段再接入或扩展

### 2. 构建后端

```powershell
mvn -pl auction-admin -am package -DskipTests
```

执行目录：`auction-backend/`

### 3. 启动应用

```powershell
java -jar auction-admin/target/auction-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/ping
```

成功返回类似：

```json
{
  "code": 0,
  "msg": "success",
  "data": "pong"
}
```

### 4. 接口文档

应用启动后可访问：

- **Knife4j**：`http://localhost:8080/doc.html`
- **OpenAPI**：`http://localhost:8080/v3/api-docs`

## 常用账号

| 用户名 | 密码 | 用途 |
|---|---|---|
| `admin` | `123456` | 管理员登录、商品审核、强制下架 |
| `bidder01` | `Bid123456` | 出价测试用户（如数据库已有） |

如果 `bidder01` 不存在，可调用注册接口创建：

```http
POST /api/system/users/register
Content-Type: application/json

{
  "username": "bidder01",
  "password": "Bid123456",
  "nickname": "bidder",
  "email": "bidder01@test.com"
}
```

## 核心接口示例

### 登录

```http
POST /api/system/users/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

### 当前登录用户

```http
GET /api/system/users/me
Authorization: Bearer <token>
```

### 分类树

```http
GET /api/categories/tree
```

### 管理端分类

```http
GET    /api/admin/categories/tree
POST   /api/admin/categories
PUT    /api/admin/categories/{id}
DELETE /api/admin/categories/{id}
PUT    /api/admin/categories/{id}/toggle-status
```

### 商品列表与详情

```http
GET /api/items?page=1&size=10
GET /api/items/{id}
```

### 发布商品

```http
POST /api/items
Authorization: Bearer <token>
Content-Type: application/json
```

商品发布后默认进入 **待审核**，管理员审核通过后进入 **待开拍/拍卖中** 流程。

### 管理员审核商品

```http
POST /api/admin/items/{id}/audit
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "action": "PASS",
  "remark": "审核通过"
}
```

审核参数说明：

- **`action=PASS`**：审核通过，商品状态进入待开拍。
- **`action=REJECT`**：审核驳回，卖家可修改后再提交。

### 管理员强制下架

```http
POST /api/admin/items/{id}/force-offline
Authorization: Bearer <admin-token>
```

### 出价

```http
POST /api/items/{itemId}/bids
Authorization: Bearer <token>
Content-Type: application/json
X-Idempotent-Key: <uuid>

{
  "price": 200.00
}
```

成功返回：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "bidId": 313637733027373056,
    "currentPrice": 200.00
  }
}
```

常见错误：

| code | 说明 |
|---|---|
| `40001` | 出价金额不足 / 低于起拍价 |
| `40002` | 不能给自己的商品出价 |
| `40003` | 拍卖未开始或已结束 |
| `40004` | 重复请求 |
| `40005` | 出价过于频繁 |
| `40006` | 出价高于一口价 |

### 出价记录

```http
GET /api/items/{itemId}/bids?page=1&size=20
```

返回记录按出价时间倒序排列，出价人显示为脱敏格式。

## 主要数据表

| 表名 | 作用 | 当前状态 |
|---|---|---|
| `sys_user` | 系统用户 | 已使用 |
| `sys_role` | 角色 | 已使用 |
| `sys_permission` | 权限 | 已使用 |
| `sys_user_role` | 用户角色关系 | 已使用 |
| `sys_role_permission` | 角色权限关系 | 已使用 |
| `sys_oper_log` | 操作日志 | 已使用 |
| `biz_category` | 商品分类 | 已使用 |
| `biz_auction_item` | 拍卖商品 | 已使用 |
| `biz_bid` | 出价记录 | 已使用 |

## Redis Key 约定

| Key | 说明 |
|---|---|
| `auction:price:{itemId}` | 商品当前价，Lua 出价读取和更新 |
| `auction:bid:queue:{itemId}` | 商品出价队列，保存最近出价 payload |
| `auction:idem:{requestId}` | 出价幂等 Key |
| `auction:item:{itemId}` | 商品信息缓存预留 |
| `cache:category:tree` | 分类树缓存 |
| `bid:rate:{userId}:{itemId}` | 出价频率限制 Key |

## 已完成的关键设计

### Redis Lua 原子出价

`auction-framework/src/main/resources/scripts/bid.lua` 实现：

1. `SETNX` 幂等检查
2. 读取 Redis 当前价
3. 校验 `newPrice >= current + increment`
4. 原子更新当前价
5. 将出价记录写入 Redis 队列

### 出价校验链

`auction-business/src/main/java/com/auction/business/bid/` 实现责任链：

```text
BidParamValidator
  -> ItemStatusValidator
  -> SelfBidValidator
  -> BidFrequencyValidator
  -> BidPriceValidator
  -> bid.lua
```

优点：

- 校验职责清晰
- 新增校验器只需新增 `@Component` 子类
- `BidServiceImpl` 主流程更简洁

### 商品状态流转

当前已实现的核心状态：

```text
卖家发布
  -> 1 待审核
  -> 管理员 PASS
  -> 2 待开拍
  -> 3 拍卖中（当前测试中可通过数据库/后续定时任务切换）
  -> 7 下架（卖家下架或管理员强制下架）
```

后续计划补充：

- **自动开拍**：审核通过后到达 `startTime` 自动切换为拍卖中。
- **自动结算**：到达 `endTime` 后通过延迟队列触发成交/流拍。

## 已验证流程

### 商品流程

- **发布商品**：登录用户发布商品，返回商品 ID。
- **列表查询**：公开访问，分页信息正确。
- **详情查询**：公开访问，浏览次数递增。
- **审核通过**：管理员审核商品，状态由待审核变为待开拍。
- **操作日志**：商品发布、审核、强制下架等操作写入 `sys_oper_log`。

### 出价流程

已验证以下场景：

| 场景 | 预期 | 实际 |
|---|---|---|
| 卖家给自己商品出价 | 阻断 | `40002` |
| 出价低于起拍价 | 阻断 | `40001` |
| 出价低于 Redis 当前价 + 加价幅度 | 阻断 | `40001` |
| 同一用户 1 秒内连续出价 | 阻断 | `40005` |
| 正常出价 | 成功 | Redis 当前价和 MySQL 当前价一致 |
| 查询出价记录 | 成功 | 出价人脱敏显示 |

## 常见问题

### 1. 数据库中文乱码

如果直接在 PowerShell 中执行包含中文的 `docker exec mysql -e "SQL"`，可能出现编码问题。

建议使用 UTF-8 SQL 文件，然后：

```powershell
docker cp deploy/mysql/xxx.sql auction-mysql:/tmp/xxx.sql
docker exec auction-mysql bash -c "mysql -uroot -proot123456 --default-character-set=utf8mb4 auction < /tmp/xxx.sql"
```

### 2. Docker 未启动

如果出现：

```text
failed to connect to the docker API
```

先启动 Docker Desktop，再执行：

```powershell
docker ps -a
```

确认 `auction-mysql`、`auction-redis` 处于 `Up` 状态。

### 3. MySQL 初始化脚本没有生效

MySQL Docker 官方镜像只在数据目录为空时执行 `/docker-entrypoint-initdb.d`。

如果修改了 `deploy/mysql/init.sql`，但容器已有数据，不会自动重跑初始化脚本。可选择：

- **保留数据**：手动执行增量 SQL。
- **重建数据**：删除 `deploy/mysql/data` 后重新启动容器（会清空数据库）。

### 4. 出价接口返回未登录

检查请求头是否为：

```http
Authorization: Bearer <token>
```

注意 `Bearer` 后有空格。

## 开发记录

| Commit | 内容 |
|---|---|
| `678e334` | JWT 鉴权与当前用户接口 |
| `684d921` | RBAC 权限模块 |
| `594de72` | 验证码、登录增强、双 Token、JWT 黑名单 |
| `e87a5f3` | AOP 日志、限流、幂等、线程池、操作日志表 |
| `7a7463d` | 分类表、分类树、分类管理接口 |
| `1bd8968` | 文件上传、本地/MinIO 存储、静态资源映射 |
| `105dba9` | 商品发布、查询、审核、MyBatis-Plus 分页/乐观锁 |
| `cde7a75` | Day 10~11：Redis Lua 原子出价、BidService、BidController、Redis 预热 |
| `817bfe1` | Day 12：出价校验链（责任链 + 模板方法） |
| `a2f251c` | Day 13：WebSocket STOMP 实时推送（依赖、配置、拦截器、WsPusher、出价广播） |
| `a76b7cf` | Day 14：反狙击延时 + 一口价（buy_now.lua、/buy-now、成交广播） |
| `本次提交` | Day 15：100并发压测、Tomcat/HikariCP/Lettuce调优、压测报告 |

## WebSocket 使用说明

### 连接方式

```text
ws://localhost:8080/api/ws?token=<jwt_access_token>
```

> 使用 SockJS 客户端（stompjs）连接，未携带 token 也可连（订阅公开主题）。

### 订阅主题

| 主题 | 触发时机 | 消息示例 |
|---|---|---|
| `/topic/auction/{itemId}` | 每次出价成功 | `{"type":"BID_PLACED","currentPrice":120,...}` |
| `/topic/auction/{itemId}/state` | 状态变化（开拍/流拍/成交） | `{"type":"STATE_CHANGE","newStatus":5,...}` |
| `/user/queue/notification` | 个人通知（被超价等） | `{"type":"NOTIFICATION","title":"..."}` |

### 前端接入示例（stompjs）

```js
const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/api/ws?token=' + token)
});
client.onConnect = () => {
  client.subscribe('/topic/auction/123456', (msg) => {
    const data = JSON.parse(msg.body);
    console.log('新出价：', data.currentPrice);
  });
};
client.activate();
```

## 下一步

继续 **Day 16：RabbitMQ 配置与消息队列**：

- RabbitConfig：声明 Exchange/Queue/Binding
- 持久化、Confirm、手动 ack
- 队列与死信队列（延迟）

## README 维护约定

后续每完成一个开发阶段或关键功能，都同步补充本 README 的：

- 项目状态
- 核心接口
- 关键设计
- 开发记录
- 下一步计划
