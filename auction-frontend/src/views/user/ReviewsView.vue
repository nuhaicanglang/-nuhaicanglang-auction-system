<script setup lang="ts">
import { onMounted, ref } from 'vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { listReviews } from '@/api/orders'
import type { Review } from '@/types/domain'
import { formatDateTime, normalizeRecords } from '@/utils/format'

const reviews = ref<Review[]>([])

onMounted(async () => {
  reviews.value = normalizeRecords(await listReviews({ page: 1, size: 30 }).catch(() => ({ records: [] })))
})
</script>

<template>
  <div class="page-stack">
    <ElTable v-if="reviews.length" :data="reviews" border>
      <ElTableColumn prop="orderId" label="订单" width="140" />
      <ElTableColumn prop="score" label="评分" width="100" />
      <ElTableColumn prop="content" label="内容" min-width="260" />
      <ElTableColumn label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
    </ElTable>
    <EmptyPanel v-else title="暂无评价" description="订单完成后可以评价交易双方。" />
  </div>
</template>
