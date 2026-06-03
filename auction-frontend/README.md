# 云槌拍卖系统前端

企业级在线拍卖系统前端，采用 Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + ECharts + Axios + STOMP/SockJS。

## 功能覆盖

- 公共端：首页、拍品大厅、全文搜索、拍品详情、实时竞价、登录、注册。
- 用户端：用户概览、发布/编辑拍品、我的拍品、买家订单、卖家订单、钱包、资金流水、信用分、评价、搜索历史。
- 管理端：运营看板、商品审核、分类管理、用户管理、角色权限、资金审计、信用管理、Excel 导出。
- 基础设施：统一请求封装、JWT 自动携带、401 静默刷新、幂等 Key、Blob 下载、权限路由、WebSocket 竞拍推送、XSS 安全展示。

## 启动

```bash
npm install
npm run dev
```

默认开发地址：

```text
http://127.0.0.1:5173/
```

默认后端地址：

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=http://localhost:8080
```

开发环境下有两类图片资源：

- 后端上传文件：`/uploads/**`，前端会自动拼接到 `VITE_API_BASE_URL`
- 前端静态样例图：`/sample-items/**`，继续由 Vite 本地静态资源提供

这样在 `5173` 前端开发服务和 `8080` 后端接口分开运行时，上传图和样例图都能正确显示。

## 验证命令

```bash
npm run typecheck
npm run lint
npm run build
```

账号密码：

| 账号 | 密码 | 用途 |
|---|---|---|
| `admin` | `123456` | 超级管理员 |
| `ops_admin` | `123456` | 运营管理员 |
| `seller_art` | `123456` | 艺术品卖家 |
| `seller_digital` | `123456` | 数码卖家 |
| `seller_luxury` | `123456` | 奢侈品卖家 |
| `buyer_vip` | `123456` | 企业采购买家 |
| `buyer_standard` | `123456` | 个人收藏买家 |
| `buyer_risk` | `123456` | 信用观察用户 |
| `blacklisted_user` | `123456` | 黑名单演示用户，不可登录 |

## 真实接口优先

前端已按当前后端真实路径接入：

- `/api/system/users/**`
- `/api/items/**`
- `/api/items/{itemId}/bids/**`
- `/api/orders/buyer`
- `/api/orders/seller`
- `/api/me/wallet/**`
- `/api/me/credit/**`
- `/api/search/**`
- `/api/upload/**`
- `/api/admin/**`
- `/api/ws?token=...`

当前已经补齐的企业级接口与页面：

- 我的收藏：`GET /api/me/favorites`
- 收藏状态/收藏操作：`GET|POST|DELETE /api/items/{id}/favorite`
- 站内信列表/已读：`GET /api/me/notifications`、`PUT /api/me/notifications/{id}/read`
- 我的竞拍汇总：`GET /api/me/bids`
- 后台订单总表：`GET /api/admin/orders`
- 操作日志查询：`GET /api/admin/logs`

当前仍属于后端待补的能力：

- 异步导出任务中心
- 更完整的站内信筛选能力
- 管理端更细粒度的操作审计聚合视图

## 设计说明

视觉方向为企业级拍卖平台：浅色专业基调、深墨色结构、金色竞拍强调色、清晰数据表格、商品卡片、倒计时、价格、状态标签和图表组件统一复用。桌面端优先，同时适配平板与移动端。
