import { request } from './request'
import type { AuctionItem, ID, SearchResult } from '@/types/domain'

export interface SearchQuery {
  keyword?: string
  categoryId?: ID
  minPrice?: string | number
  maxPrice?: string | number
  status?: number
  sort?: string
  order?: string
  saveHistory?: boolean
  page?: number
  size?: number
}

export function searchItems(params: SearchQuery) {
  return request<SearchResult>({
    url: '/api/search/items',
    method: 'GET',
    params,
    skipAuth: !params.saveHistory,
  })
}

export function suggestItems(prefix: string, size = 8) {
  return request<AuctionItem[]>({
    url: '/api/search/suggest',
    method: 'GET',
    params: { prefix, size },
    skipAuth: true,
    silent: true,
  })
}

export function getSearchHistory() {
  return request<string[]>({
    url: '/api/search/history',
    method: 'GET',
  })
}

export function clearSearchHistory() {
  return request<void>({
    url: '/api/search/history',
    method: 'DELETE',
  })
}
