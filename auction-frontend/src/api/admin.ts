import { downloadExcel, request } from './request'
import type {
  AuctionItem,
  AuctionItemQuery,
  Category,
  Credit,
  CreditLog,
  ID,
  OperLog,
  Order,
  PageQuery,
  PageResult,
  Permission,
  Role,
  StatsOverview,
  StatsTrendPoint,
  TopItem,
  User,
  WalletSummary,
  WalletTransaction,
} from '@/types/domain'

export function listAuditItems(params: AuctionItemQuery = {}) {
  return request<PageResult<AuctionItem>>({
    url: '/api/admin/items/audits',
    method: 'GET',
    params,
  })
}

export function auditItem(id: ID, data: { action: 'PASS' | 'REJECT'; remark?: string }) {
  return request<void>({
    url: `/api/admin/items/${id}/audit`,
    method: 'POST',
    data,
  })
}

export function forceOfflineItem(id: ID) {
  return request<void>({
    url: `/api/admin/items/${id}/force-offline`,
    method: 'POST',
  })
}

export function listAdminOrders(params: PageQuery & { orderNo?: string; buyerId?: ID; sellerId?: ID } = {}) {
  return request<PageResult<Order>>({
    url: '/api/admin/orders',
    method: 'GET',
    params,
  })
}

export function listOperLogs(params: PageQuery & { module?: string; businessType?: string; operUserName?: string } = {}) {
  return request<PageResult<OperLog>>({
    url: '/api/admin/logs',
    method: 'GET',
    params,
  })
}

export function seedSampleItems(data: { count?: number }) {
  return request<{ count: number; itemIds: ID[] }>({
    url: '/api/admin/items/sample-batch',
    method: 'POST',
    data,
    idempotent: true,
  })
}

export function listAdminCategories() {
  return request<Category[]>({
    url: '/api/admin/categories/tree',
    method: 'GET',
  })
}

export function createCategory(data: Partial<Category>) {
  return request<void>({
    url: '/api/admin/categories',
    method: 'POST',
    data,
  })
}

export function updateCategory(id: ID, data: Partial<Category>) {
  return request<void>({
    url: `/api/admin/categories/${id}`,
    method: 'PUT',
    data,
  })
}

export function toggleCategoryStatus(id: ID) {
  return request<void>({
    url: `/api/admin/categories/${id}/toggle-status`,
    method: 'PUT',
  })
}

export function listUsers() {
  return request<User[]>({
    url: '/api/admin/users',
    method: 'GET',
  })
}

export function changeUserStatus(userId: ID, status: number) {
  return request<void>({
    url: `/api/admin/users/${userId}/status`,
    method: 'PUT',
    params: { status },
  })
}

export function blacklistUser(userId: ID, reason: string) {
  return request<void>({
    url: `/api/admin/users/${userId}/blacklist`,
    method: 'POST',
    data: { reason },
  })
}

export function unblacklistUser(userId: ID) {
  return request<void>({
    url: `/api/admin/users/${userId}/blacklist`,
    method: 'DELETE',
  })
}

export function listRoles() {
  return request<Role[]>({
    url: '/api/admin/roles',
    method: 'GET',
  })
}

export function listPermissions() {
  return request<Permission[]>({
    url: '/api/admin/permissions',
    method: 'GET',
  })
}

export function getRolePermissions(roleId: ID) {
  return request<ID[]>({
    url: `/api/admin/roles/${roleId}/permissions`,
    method: 'GET',
  })
}

export function assignRolePermissions(roleId: ID, permissionIds: ID[]) {
  return request<void>({
    url: `/api/admin/roles/${roleId}/permissions`,
    method: 'PUT',
    data: permissionIds,
  })
}

export function getStatsOverview() {
  return request<StatsOverview>({
    url: '/api/admin/stats/overview',
    method: 'GET',
  })
}

export function getStatsTrend(days = 30) {
  return request<StatsTrendPoint[]>({
    url: '/api/admin/stats/trend',
    method: 'GET',
    params: { days },
  })
}

export function getHotCategories(limit = 10) {
  return request<Array<{ categoryId: ID; categoryName: string; itemCount?: number }>>({
    url: '/api/admin/stats/categories',
    method: 'GET',
    params: { limit },
  })
}

export function getTopItems(limit = 10) {
  return request<TopItem[]>({
    url: '/api/admin/stats/items/top',
    method: 'GET',
    params: { limit },
  })
}

export function adminAdjustWallet(userId: ID, data: { actionType: string; amount: string; adminPassword: string; remark: string }) {
  return request<void>({
    url: `/api/admin/users/${userId}/wallet/adjust`,
    method: 'POST',
    data,
    idempotent: true,
  })
}

export function listAllWalletTransactions(params: PageQuery & { userId?: ID; actionType?: string; bizType?: string } = {}) {
  return request<PageResult<WalletTransaction>>({
    url: '/api/admin/wallet/transactions',
    method: 'GET',
    params,
  })
}

export function getWalletSummary() {
  return request<WalletSummary>({
    url: '/api/admin/wallet/summary',
    method: 'GET',
  })
}

export function getUserCredit(userId: ID) {
  return request<Credit>({
    url: `/api/admin/credit/users/${userId}`,
    method: 'GET',
  })
}

export function listAllCreditLogs(params: PageQuery & { userId?: ID; eventType?: string } = {}) {
  return request<PageResult<CreditLog>>({
    url: '/api/admin/credit/logs',
    method: 'GET',
    params,
  })
}

export function adminAdjustCredit(userId: ID, data: { deltaScore: number; remark: string }) {
  return request<void>({
    url: `/api/admin/credit/users/${userId}/adjust`,
    method: 'POST',
    data,
    idempotent: true,
  })
}

export const exportApi = {
  orders: (params?: Record<string, unknown>) => downloadExcel('/api/admin/export/orders', 'orders.xlsx', params),
  users: (params?: Record<string, unknown>) => downloadExcel('/api/admin/export/users', 'users.xlsx', params),
  wallet: (params?: Record<string, unknown>) => downloadExcel('/api/admin/export/wallet', 'wallet-transactions.xlsx', params),
}
