export type ID = string | number
export type DateTimeString = string

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  records?: T[]
  list?: T[]
  items?: T[]
  total?: number | string
  current?: number | string
  page?: number | string
  size?: number | string
  pages?: number | string
}

export interface PageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number | string
}

export interface User {
  id: ID
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  gender?: number
  status?: number
  createdAt?: DateTimeString
  roles?: string[]
}

export interface Role {
  id: ID
  code: string
  name: string
  description?: string
  sortOrder?: number
  status?: number
  createdAt?: DateTimeString
}

export interface Permission {
  id: ID
  parentId?: ID
  code: string
  name: string
  type?: number
  path?: string
  icon?: string
  sortOrder?: number
  status?: number
  children?: Permission[]
}

export interface LoginPayload {
  username: string
  password: string
  captchaUuid?: string
  captchaCode?: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}

export interface LoginResult {
  token: string
  refreshToken: string
  user: User
  roles: string[]
}

export interface CaptchaResult {
  uuid?: string
  captchaUuid?: string
  image?: string
  img?: string
  base64?: string
}

export interface Category {
  id: ID
  parentId?: ID
  path?: string
  level?: number
  name: string
  icon?: string
  description?: string
  sortOrder?: number
  status?: number
  itemCount?: number
  children?: Category[]
}

export interface AuctionItem {
  id: ID
  title: string
  subtitle?: string
  description?: string
  categoryId?: ID
  categoryPath?: string
  coverImage?: string
  images?: string[]
  sellerId?: ID
  sellerName?: string
  sellerAvatar?: string
  auctionType?: number
  startPrice?: string | number
  currentPrice?: string | number
  bidIncrement?: string | number
  buyNowPrice?: string | number
  deposit?: string | number
  startTime?: DateTimeString
  endTime?: DateTimeString
  actualEndTime?: DateTimeString
  status?: number
  statusText?: string
  bidCount?: number
  viewCount?: number
  favoriteCount?: number
  createdAt?: DateTimeString
}

export interface AuctionItemQuery extends PageQuery {
  categoryId?: ID
  priceMin?: string | number
  priceMax?: string | number
  sellerId?: ID
  sort?: string
}

export interface AuctionItemCreatePayload {
  title: string
  subtitle?: string
  description: string
  categoryId?: ID
  coverImage: string
  images: string[]
  startPrice: string
  bidIncrement: string
  buyNowPrice?: string
  deposit: string
  duration: number
  startMode: 'IMMEDIATE' | 'SCHEDULED'
  scheduledStartTime?: string
}

export interface Bid {
  id: ID
  itemId: ID
  bidderId?: ID
  bidderName?: string
  bidPrice: string | number
  bidTime?: DateTimeString
  bidType?: number
  status?: number
}

export interface MyBid {
  id: ID
  itemId: ID
  itemTitle?: string
  itemCoverImage?: string
  sellerId?: ID
  myBidPrice?: string | number
  currentPrice?: string | number
  buyNowPrice?: string | number
  bidTime?: DateTimeString
  endTime?: DateTimeString
  bidType?: number
  bidStatus?: number
  itemStatus?: number
  itemStatusText?: string
  resultCode?: string
  resultText?: string
}

export interface BidResult {
  bidId?: ID
  currentPrice?: string | number
  extended?: boolean
  endTime?: DateTimeString
  deal?: boolean
  status?: number
}

export interface Order {
  id: ID
  orderNo: string
  itemId: ID
  itemTitle: string
  itemCoverImage?: string
  buyerId?: ID
  sellerId?: ID
  bidId?: ID
  dealPrice?: string | number
  depositAmount?: string | number
  payAmount?: string | number
  status?: number
  statusText?: string
  payDeadline?: DateTimeString
  paidAt?: DateTimeString
  shippedAt?: DateTimeString
  completedAt?: DateTimeString
  closedAt?: DateTimeString
  closeReason?: string
  createdAt?: DateTimeString
}

export interface Notification {
  id: ID
  type?: number
  title: string
  content?: string
  relatedItemId?: ID
  isRead?: number
  readAt?: DateTimeString
  createdAt?: DateTimeString
}

export interface OperLog {
  id: ID
  traceId?: string
  module?: string
  businessType?: string
  description?: string
  method?: string
  requestUrl?: string
  requestMethod?: string
  operUserId?: ID
  operUserName?: string
  operIp?: string
  status?: number
  errorMsg?: string
  costMs?: number
  createdAt?: DateTimeString
}

export interface Wallet {
  id?: ID
  userId?: ID
  balance?: string | number
  frozenBalance?: string | number
  totalAmount?: string | number
  status?: number
  updatedAt?: DateTimeString
}

export interface WalletTransaction {
  id: ID
  transactionNo?: string
  userId?: ID
  actionType?: string
  direction?: number
  amount?: string | number
  balanceBefore?: string | number
  balanceAfter?: string | number
  frozenBefore?: string | number
  frozenAfter?: string | number
  bizType?: string
  bizId?: string
  relatedItemId?: ID
  operatorId?: ID
  remark?: string
  createdAt?: DateTimeString
}

export interface WalletSummary {
  userCount?: number
  activeWalletCount?: number
  totalBalance?: string | number
  totalFrozenBalance?: string | number
  totalAmount?: string | number
}

export interface Credit {
  userId?: ID
  score?: number
  levelName?: string
  status?: number
  lastEventAt?: DateTimeString
  updatedAt?: DateTimeString
}

export interface CreditLog {
  id: ID
  userId?: ID
  eventType?: string
  relatedId?: string
  deltaScore?: number
  scoreBefore?: number
  scoreAfter?: number
  remark?: string
  createdAt?: DateTimeString
}

export interface Review {
  id: ID
  orderId?: ID
  itemId?: ID
  reviewerId?: ID
  revieweeId?: ID
  roleType?: string
  score?: number
  content?: string
  status?: number
  createdAt?: DateTimeString
}

export interface SearchResult {
  total?: number
  page?: number
  size?: number
  items?: AuctionItem[]
  categoryFacets?: Facet[]
  statusFacets?: Facet[]
  priceFacets?: Facet[]
}

export interface SearchHit extends AuctionItem {
  highlightTitle?: string
  highlightSubtitle?: string
}

export interface Facet {
  key: string
  count: number
}

export interface StatsOverview {
  todayUserCount?: number
  todayItemCount?: number
  todayOrderCount?: number
  todayDealAmount?: string | number
  runningItemCount?: number
  pendingAuditCount?: number
  totalUserCount?: number
  totalDealAmount?: string | number
}

export interface StatsTrendPoint {
  date: string
  userCount?: number
  itemCount?: number
  orderCount?: number
  dealAmount?: string | number
}

export interface TopItem {
  itemId: ID
  title: string
  categoryId?: ID
  currentPrice?: string | number
  finalPrice?: string | number
  bidCount?: number
  viewCount?: number
  status?: number
}

export interface UploadResult {
  url?: string
  urls?: string[]
  name?: string
  size?: number
}
