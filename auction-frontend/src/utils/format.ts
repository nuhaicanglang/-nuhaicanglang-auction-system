import Decimal from 'decimal.js'
import type { PageResult } from '@/types/domain'

export function toDecimal(value?: string | number | null) {
  if (value === null || value === undefined || value === '') {
    return new Decimal(0)
  }
  return new Decimal(value)
}

export function formatMoney(value?: string | number | null) {
  return `¥${toDecimal(value).toFixed(2)}`
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function secondsUntil(value?: string | null, now = Date.now()) {
  if (!value) {
    return 0
  }
  const end = new Date(value).getTime()
  if (Number.isNaN(end)) {
    return 0
  }
  return Math.max(0, Math.floor((end - now) / 1000))
}

export function formatDuration(seconds: number) {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (days > 0) {
    return `${days}天 ${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
  }
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

export function normalizeRecords<T>(page?: PageResult<T> | T[]): T[] {
  if (Array.isArray(page)) {
    return page
  }
  return page?.records ?? page?.list ?? page?.items ?? []
}

export function normalizeTotal<T>(page?: PageResult<T> | T[]) {
  if (Array.isArray(page)) {
    return page.length
  }
  return page?.total ?? normalizeRecords(page).length
}

export function statusType(status?: number | string) {
  const value = String(status ?? '')
  if (['3', 'PASS', '已支付', '已完成', 'ACTIVE'].includes(value)) {
    return 'success'
  }
  if (['1', '待支付', '待审核', 'PENDING'].includes(value)) {
    return 'warning'
  }
  if (['7', 'REJECT', '已取消', '禁用', 'BLACKLISTED'].includes(value)) {
    return 'danger'
  }
  return 'info'
}

export function safeText(value?: string | number | null, fallback = '-') {
  return value === undefined || value === null || value === '' ? fallback : String(value)
}
