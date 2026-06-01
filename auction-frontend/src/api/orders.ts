import { request } from './request'
import type { ID, Order, PageQuery, PageResult, Review } from '@/types/domain'

export interface PaymentVO {
  id?: ID
  paymentNo?: string
  orderId?: ID
  orderNo?: string
  payerId?: ID
  amount?: string | number
  payMethod?: string
  status?: number
  statusText?: string
  paidAt?: string
  createdAt?: string
}

export function listBuyerOrders(params: PageQuery = {}) {
  return request<PageResult<Order>>({
    url: '/api/orders/buyer',
    method: 'GET',
    params,
  })
}

export function listSellerOrders(params: PageQuery = {}) {
  return request<PageResult<Order>>({
    url: '/api/orders/seller',
    method: 'GET',
    params,
  })
}

export function getOrder(id: ID) {
  return request<Order>({
    url: `/api/orders/${id}`,
    method: 'GET',
  })
}

export function payOrder(id: ID) {
  return request<PaymentVO>({
    url: `/api/orders/${id}/pay`,
    method: 'POST',
    idempotent: true,
  })
}

export function shipOrder(id: ID) {
  return request<void>({
    url: `/api/orders/${id}/ship`,
    method: 'POST',
  })
}

export function completeOrder(id: ID) {
  return request<void>({
    url: `/api/orders/${id}/complete`,
    method: 'POST',
  })
}

export function createReview(orderId: ID, data: { score: number; content: string }) {
  return request<Review>({
    url: `/api/orders/${orderId}/review`,
    method: 'POST',
    data,
  })
}

export function listReviews(params: PageQuery & { userId?: ID; itemId?: ID } = {}) {
  return request<PageResult<Review>>({
    url: '/api/reviews',
    method: 'GET',
    params,
  })
}
