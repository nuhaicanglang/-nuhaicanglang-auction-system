import { request } from './request'
import type { ID, Notification, PageQuery, PageResult } from '@/types/domain'

export function listNotifications(params: PageQuery & { isRead?: number } = {}) {
  return request<PageResult<Notification>>({
    url: '/api/me/notifications',
    method: 'GET',
    params,
  })
}

export function markNotificationRead(id: ID) {
  return request<void>({
    url: `/api/me/notifications/${id}/read`,
    method: 'PUT',
  })
}
