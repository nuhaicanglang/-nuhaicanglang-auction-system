import { request } from './request'
import type { Credit, CreditLog, PageQuery, PageResult, Wallet, WalletTransaction } from '@/types/domain'

export function getMyWallet() {
  return request<Wallet>({
    url: '/api/me/wallet',
    method: 'GET',
  })
}

export function listMyWalletTransactions(params: PageQuery & { actionType?: string; bizType?: string } = {}) {
  return request<PageResult<WalletTransaction>>({
    url: '/api/me/wallet/transactions',
    method: 'GET',
    params,
  })
}

export function getMyCredit() {
  return request<Credit>({
    url: '/api/me/credit',
    method: 'GET',
  })
}

export function listMyCreditLogs(params: PageQuery & { eventType?: string } = {}) {
  return request<PageResult<CreditLog>>({
    url: '/api/me/credit/logs',
    method: 'GET',
    params,
  })
}
