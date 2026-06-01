import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'

const PublicLayout = () => import('@/layouts/PublicLayout.vue')
const WorkspaceLayout = () => import('@/layouts/WorkspaceLayout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: PublicLayout,
    children: [
      { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue'), meta: { title: '首页' } },
      { path: 'items', name: 'items', component: () => import('@/views/public/ItemListView.vue'), meta: { title: '拍品列表' } },
      { path: 'items/:id', name: 'item-detail', component: () => import('@/views/public/ItemDetailView.vue'), meta: { title: '拍品详情' } },
      { path: 'search', name: 'search', component: () => import('@/views/public/SearchView.vue'), meta: { title: '搜索拍品' } },
      { path: 'login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { title: '登录' } },
      { path: 'register', name: 'register', component: () => import('@/views/auth/RegisterView.vue'), meta: { title: '注册' } },
      { path: '403', name: 'forbidden', component: () => import('@/views/system/ForbiddenView.vue'), meta: { title: '无权限' } },
      { path: '404', name: 'not-found', component: () => import('@/views/system/NotFoundView.vue'), meta: { title: '未找到页面' } },
    ],
  },
  {
    path: '/user',
    component: WorkspaceLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/user/overview' },
      { path: 'overview', name: 'user-overview', component: () => import('@/views/user/UserOverviewView.vue'), meta: { title: '用户中心', requiresAuth: true, menu: true } },
      { path: 'publish', name: 'user-publish', component: () => import('@/views/user/ItemFormView.vue'), meta: { title: '发布拍品', requiresAuth: true, menu: true } },
      { path: 'items', name: 'user-items', component: () => import('@/views/user/MyItemsView.vue'), meta: { title: '我的拍品', requiresAuth: true, menu: true } },
      { path: 'items/:id/edit', name: 'user-item-edit', component: () => import('@/views/user/ItemFormView.vue'), meta: { title: '编辑拍品', requiresAuth: true } },
      { path: 'bids', name: 'user-bids', component: () => import('@/views/user/MyBidsView.vue'), meta: { title: '我的竞拍', requiresAuth: true, menu: true } },
      { path: 'orders/buyer', name: 'buyer-orders', component: () => import('@/views/user/OrdersView.vue'), meta: { title: '我买到的', requiresAuth: true, menu: true } },
      { path: 'orders/seller', name: 'seller-orders', component: () => import('@/views/user/OrdersView.vue'), meta: { title: '我卖出的', requiresAuth: true, menu: true } },
      { path: 'wallet', name: 'wallet', component: () => import('@/views/user/WalletView.vue'), meta: { title: '我的钱包', requiresAuth: true, menu: true } },
      { path: 'credit', name: 'credit', component: () => import('@/views/user/CreditView.vue'), meta: { title: '信用分', requiresAuth: true, menu: true } },
      { path: 'reviews', name: 'reviews', component: () => import('@/views/user/ReviewsView.vue'), meta: { title: '评价管理', requiresAuth: true, menu: true } },
      { path: 'search-history', name: 'search-history', component: () => import('@/views/user/SearchHistoryView.vue'), meta: { title: '搜索历史', requiresAuth: true, menu: true } },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/system/BacklogView.vue'), meta: { title: '我的收藏', requiresAuth: true, menu: true } },
      { path: 'notifications', name: 'notifications', component: () => import('@/views/system/BacklogView.vue'), meta: { title: '站内信', requiresAuth: true, menu: true } },
    ],
  },
  {
    path: '/admin',
    component: WorkspaceLayout,
    meta: { requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'] },
    children: [
      { path: '', redirect: '/admin/stats' },
      { path: 'stats', name: 'admin-stats', component: () => import('@/views/admin/StatsDashboardView.vue'), meta: { title: '运营看板', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'items/audits', name: 'admin-audits', component: () => import('@/views/admin/ItemAuditView.vue'), meta: { title: '商品审核', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/CategoryManageView.vue'), meta: { title: '分类管理', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UserManageView.vue'), meta: { title: '用户管理', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'roles', name: 'admin-roles', component: () => import('@/views/admin/RolePermissionView.vue'), meta: { title: '角色权限', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'wallet', name: 'admin-wallet', component: () => import('@/views/admin/AdminWalletView.vue'), meta: { title: '资金审计', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'credit', name: 'admin-credit', component: () => import('@/views/admin/AdminCreditView.vue'), meta: { title: '信用管理', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'exports', name: 'admin-exports', component: () => import('@/views/admin/ExportCenterView.vue'), meta: { title: '数据导出', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'orders', name: 'admin-orders', component: () => import('@/views/system/BacklogView.vue'), meta: { title: '订单总表', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
      { path: 'logs', name: 'admin-logs', component: () => import('@/views/system/BacklogView.vue'), meta: { title: '操作日志', requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'], menu: true } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/404' },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  document.title = `${to.meta.title ? `${to.meta.title} - ` : ''}云槌拍卖系统`

  const auth = useAuthStore(pinia)
  auth.syncFromStorage()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (auth.isAuthenticated && !auth.user) {
    try {
      await auth.loadProfile()
    } catch {
      await auth.logout()
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      }
    }
  }

  const allowedRoles = to.meta.roles
  if (allowedRoles?.length && !allowedRoles.some((role) => auth.roles.includes(role))) {
    return '/403'
  }

  return true
})
