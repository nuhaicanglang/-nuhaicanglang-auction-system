<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AuctionCard from '@/components/business/AuctionCard.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { listMyFavorites } from '@/api/items'
import type { AuctionItem } from '@/types/domain'
import { normalizeRecords, normalizeTotal } from '@/utils/format'

const loading = ref(false)
const items = ref<AuctionItem[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 12,
})

async function loadFavorites() {
  loading.value = true
  try {
    const page = await listMyFavorites(query)
    items.value = normalizeRecords(page)
    total.value = normalizeTotal(page)
  } finally {
    loading.value = false
  }
}

onMounted(loadFavorites)
</script>

<template>
  <div class="page-stack">
    <div v-if="items.length" v-loading="loading" class="auction-grid">
      <AuctionCard v-for="item in items" :key="item.id" :item="item" />
    </div>
    <EmptyPanel v-else title="暂无收藏" description="在拍品详情页点击收藏后，会出现在这里。" />

    <ElPagination
      v-if="total > query.size"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      layout="total, prev, pager, next"
      :total="total"
      @current-change="loadFavorites"
    />
  </div>
</template>

<style scoped>
.auction-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1000px) {
  .auction-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .auction-grid {
    grid-template-columns: 1fr;
  }
}
</style>
