import { request } from './request'
import type {
  AuctionItem,
  AuctionItemCreatePayload,
  AuctionItemQuery,
  Bid,
  BidResult,
  Category,
  ID,
  PageResult,
} from '@/types/domain'

export function listCategories() {
  return request<Category[]>({
    url: '/api/categories/tree',
    method: 'GET',
    skipAuth: true,
  })
}

export function listItems(params: AuctionItemQuery = {}) {
  return request<PageResult<AuctionItem>>({
    url: '/api/items',
    method: 'GET',
    params,
    skipAuth: true,
  })
}

export function getItem(id: ID) {
  return request<AuctionItem>({
    url: `/api/items/${id}`,
    method: 'GET',
    skipAuth: true,
  })
}

export function publishItem(data: AuctionItemCreatePayload) {
  return request<ID>({
    url: '/api/items',
    method: 'POST',
    data,
  })
}

export function updateItem(id: ID, data: AuctionItemCreatePayload) {
  return request<void>({
    url: `/api/items/${id}`,
    method: 'PUT',
    data,
  })
}

export function offlineItem(id: ID) {
  return request<void>({
    url: `/api/items/${id}/offline`,
    method: 'POST',
  })
}

export function listMyItems(params: AuctionItemQuery = {}) {
  return request<PageResult<AuctionItem>>({
    url: '/api/items/my',
    method: 'GET',
    params,
  })
}

export function listBids(itemId: ID, params = { page: 1, size: 20 }) {
  return request<PageResult<Bid>>({
    url: `/api/items/${itemId}/bids`,
    method: 'GET',
    params,
    skipAuth: true,
  })
}

export function placeBid(itemId: ID, price: string) {
  return request<BidResult>({
    url: `/api/items/${itemId}/bids`,
    method: 'POST',
    data: { price },
    idempotent: true,
  })
}

export function buyNow(itemId: ID) {
  return request<BidResult>({
    url: `/api/items/${itemId}/bids/buy-now`,
    method: 'POST',
    idempotent: true,
  })
}
