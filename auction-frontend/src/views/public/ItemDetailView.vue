<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import DOMPurify from 'dompurify'
import BidPanel from '@/components/business/BidPanel.vue'
import CountdownBadge from '@/components/common/CountdownBadge.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import PriceText from '@/components/common/PriceText.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getItem, listBids } from '@/api/items'
import { useAuthStore } from '@/stores/auth'
import type { AuctionItem, Bid } from '@/types/domain'
import { AuctionSocket } from '@/services/auctionSocket'
import { formatDateTime, normalizeRecords } from '@/utils/format'

const route = useRoute()
const auth = useAuthStore()
const item = ref<AuctionItem | null>(null)
const bids = ref<Bid[]>([])
const socket = new AuctionSocket()

const cleanDescription = computed(() => DOMPurify.sanitize(item.value?.description || '<p>暂无详情描述。</p>'))

async function loadDetail() {
  const id = String(route.params.id)
  item.value = await getItem(id)
  const page = await listBids(id)
  bids.value = normalizeRecords(page)
}

onMounted(async () => {
  await loadDetail()
  const id = String(route.params.id)
  socket.connect()
  socket.subscribeItem(id, {
    onBid: () => loadDetail(),
    onState: () => loadDetail(),
  })
})

onBeforeUnmount(() => socket.disconnect())
</script>

<template>
  <div class="page-shell detail-view">
    <template v-if="item">
      <section class="detail-grid">
        <div class="gallery surface">
          <div class="cover" :style="item.coverImage ? { backgroundImage: `url(${item.coverImage})` } : {}">
            <span v-if="!item.coverImage">待上传封面</span>
          </div>
          <div class="thumbs">
            <img v-for="url in item.images" :key="url" :src="url" alt="拍品详情图" />
          </div>
        </div>

        <section class="info surface">
          <div class="title-row">
            <StatusTag :status="item.status" :text="item.statusText" />
            <CountdownBadge :end-time="item.actualEndTime || item.endTime" />
          </div>
          <h1>{{ item.title }}</h1>
          <p>{{ item.subtitle || '公开竞价，价高者得。' }}</p>
          <div class="price-strip">
            <PriceText :value="item.currentPrice ?? item.startPrice" label="当前价" strong />
            <PriceText :value="item.startPrice" label="起拍价" />
            <PriceText :value="item.buyNowPrice" label="一口价" />
          </div>
          <dl class="meta-list">
            <div><dt>分类</dt><dd>{{ item.categoryPath || '-' }}</dd></div>
            <div><dt>卖家</dt><dd>{{ item.sellerName || `用户 ${item.sellerId ?? '-'}` }}</dd></div>
            <div><dt>开始时间</dt><dd>{{ formatDateTime(item.startTime) }}</dd></div>
            <div><dt>结束时间</dt><dd>{{ formatDateTime(item.actualEndTime || item.endTime) }}</dd></div>
          </dl>
        </section>

        <BidPanel :item="item" :authenticated="auth.isAuthenticated" @success="loadDetail" />
      </section>

      <section class="content-grid">
        <article class="description surface">
          <h2>拍品详情</h2>
          <div class="rich-text" v-html="cleanDescription" />
        </article>

        <aside class="bid-history surface">
          <h2>出价记录</h2>
          <ElTimeline v-if="bids.length">
            <ElTimelineItem v-for="bid in bids" :key="bid.id" :timestamp="formatDateTime(bid.bidTime)">
              <strong class="money">{{ bid.bidderName || '匿名竞买人' }}</strong>
              出价
              <span class="money">{{ bid.bidPrice }}</span>
            </ElTimelineItem>
          </ElTimeline>
          <EmptyPanel v-else title="暂无出价" description="成为第一个出价的竞买人。" />
        </aside>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-view {
  padding: 30px 0 56px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr) 330px;
  gap: 18px;
  align-items: start;
}

.gallery,
.info,
.description,
.bid-history {
  padding: 18px;
}

.cover {
  display: grid;
  min-height: 430px;
  place-items: center;
  border-radius: var(--radius-md);
  background:
    linear-gradient(135deg, rgba(18, 54, 72, 0.9), rgba(183, 128, 44, 0.76)),
    #203543;
  background-position: center;
  background-size: cover;
  color: #fffaf1;
  font-weight: 700;
}

.thumbs {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.thumbs img {
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  object-fit: cover;
}

.title-row,
.price-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.info h1 {
  margin: 18px 0 10px;
  font-family: var(--font-display);
  font-size: 36px;
  line-height: 1.18;
}

.info p {
  color: var(--color-muted);
  line-height: 1.7;
}

.price-strip {
  margin: 20px 0;
  padding: 16px;
  border-radius: var(--radius-md);
  background: #f8f1e6;
}

.meta-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.meta-list div {
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 10px;
}

dt {
  color: var(--color-muted);
}

dd {
  margin: 0;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 18px;
  margin-top: 18px;
}

h2 {
  margin: 0 0 14px;
}

.rich-text {
  color: var(--color-ink);
  line-height: 1.8;
}

@media (max-width: 1180px) {
  .detail-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
