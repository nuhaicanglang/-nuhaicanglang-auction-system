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

## 验证命令

```bash
npm run typecheck
npm run lint
npm run build
```

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

以下企业级能力已预留页面入口，但等待后端公开查询接口：

- 我的收藏：`GET /api/me/favorites`
- 站内信列表/已读：`GET /api/me/notifications`
- 我的竞拍汇总：`GET /api/me/bids`
- 后台订单总表：`GET /api/admin/orders`
- 操作日志查询：`GET /api/admin/logs`

## 设计说明

视觉方向为企业级拍卖平台：浅色专业基调、深墨色结构、金色竞拍强调色、清晰数据表格、商品卡片、倒计时、价格、状态标签和图表组件统一复用。桌面端优先，同时适配平板与移动端。
