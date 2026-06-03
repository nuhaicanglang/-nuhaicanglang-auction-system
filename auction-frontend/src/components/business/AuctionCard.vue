<script setup lang="ts">
import { computed } from 'vue'
import type { AuctionItem } from '@/types/domain'
import CountdownBadge from '@/components/common/CountdownBadge.vue'
import PriceText from '@/components/common/PriceText.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { resolveAssetUrl } from '@/utils/assets'

const props = defineProps<{
  item: AuctionItem
}>()

const coverStyle = computed(() => {
  if (!props.item.coverImage) {
    return {}
  }
  return { backgroundImage: `url(${resolveAssetUrl(props.item.coverImage)})` }
})
</script>

<template>
  <RouterLink class="auction-card surface" :to="`/items/${item.id}`">
    <div class="cover" :style="coverStyle">
      <span v-if="!item.coverImage">待上传封面</span>
      <StatusTag class="status" :status="item.status" :text="item.statusText" />
    </div>
    <div class="body">
      <p class="category">{{ item.categoryPath || '精选拍品' }}</p>
      <h2>{{ item.title }}</h2>
      <p class="subtitle">{{ item.subtitle || '查看详情、出价记录和实时竞拍状态。' }}</p>
      <div class="meta">
        <PriceText :value="item.currentPrice ?? item.startPrice" label="当前价" strong />
        <CountdownBadge :end-time="item.actualEndTime || item.endTime" />
      </div>
      <div class="stats">
        <span>{{ item.bidCount ?? 0 }} 次出价</span>
        <span>{{ item.viewCount ?? 0 }} 次围观</span>
        <span>{{ item.favoriteCount ?? 0 }} 收藏</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.auction-card {
  display: grid;
  overflow: hidden;
  color: inherit;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.auction-card:hover,
.auction-card:focus-visible {
  border-color: rgba(183, 128, 44, 0.55);
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
}

.cover {
  position: relative;
  display: grid;
  min-height: 212px;
  place-items: center;
  background:
    linear-gradient(135deg, rgba(18, 54, 72, 0.88), rgba(183, 128, 44, 0.72)),
    #203543;
  background-position: center;
  background-size: cover;
  color: rgba(255, 253, 248, 0.86);
  font-weight: 700;
}

.status {
  position: absolute;
  top: 12px;
  right: 12px;
}

.body {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.category {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 13px;
  font-weight: 700;
}

h2 {
  min-height: 54px;
  margin: 0;
  color: var(--color-ink);
  font-size: 20px;
  line-height: 1.35;
}

.subtitle {
  min-height: 44px;
  margin: 0;
  color: var(--color-muted);
  line-height: 1.55;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--color-muted);
  font-size: 13px;
}

@media (max-width: 720px) {
  .cover {
    min-height: 180px;
  }
}
</style>
