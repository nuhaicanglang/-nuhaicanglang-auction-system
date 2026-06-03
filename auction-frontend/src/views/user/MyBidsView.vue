<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { listMyBids } from '@/api/items'
import type { MyBid } from '@/types/domain'
import { resolveAssetUrl } from '@/utils/assets'
import { formatDateTime, formatMoney, normalizeRecords, normalizeTotal, safeText } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const total = ref(0)
const records = ref<MyBid[]>([])
const query = reactive({
  page: 1,
  size: 10,
})

const stats = computed(() => ({
  total: records.value.length,
  leading: records.value.filter((item) => item.resultCode === 'LEADING').length,
  won: records.value.filter((item) => item.resultCode === 'WON').length,
  waiting: records.value.filter((item) => ['WAITING', 'PENDING'].includes(String(item.resultCode))).length,
}))

async function loadMyBids() {
  loading.value = true
  try {
    const page = await listMyBids(query)
    records.value = normalizeRecords(page)
    total.value = normalizeTotal(page)
  } finally {
    loading.value = false
  }
}

function goDetail(itemId?: string | number) {
  if (!itemId) return
  router.push(`/items/${itemId}`)
}

onMounted(loadMyBids)
</script>

<template>
  <div class="page-stack">
    <section class="bid-summary">
      <article>
        <span>竞拍记录</span>
        <strong>{{ total }}</strong>
      </article>
      <article>
        <span>当前领先</span>
        <strong>{{ stats.leading }}</strong>
      </article>
      <article>
        <span>已中标</span>
        <strong>{{ stats.won }}</strong>
      </article>
      <article>
        <span>待开拍/待审核</span>
        <strong>{{ stats.waiting }}</strong>
      </article>
    </section>

    <ElTable v-if="records.length" v-loading="loading" :data="records" border>
      <ElTableColumn label="拍品" min-width="320">
        <template #default="{ row }">
          <div class="item-cell">
            <img v-if="row.itemCoverImage" :src="resolveAssetUrl(row.itemCoverImage)" :alt="row.itemTitle || '拍品'" />
            <div>
              <button type="button" class="item-link" @click="goDetail(row.itemId)">{{ safeText(row.itemTitle) }}</button>
              <p class="muted">结束时间：{{ formatDateTime(row.endTime) }}</p>
            </div>
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="我的出价" width="130">
        <template #default="{ row }">{{ formatMoney(row.myBidPrice) }}</template>
      </ElTableColumn>
      <ElTableColumn label="当前价" width="130">
        <template #default="{ row }">{{ formatMoney(row.currentPrice) }}</template>
      </ElTableColumn>
      <ElTableColumn label="拍卖状态" width="120">
        <template #default="{ row }"><StatusTag :status="row.itemStatus" :text="row.itemStatusText" /></template>
      </ElTableColumn>
      <ElTableColumn label="我的结果" width="120">
        <template #default="{ row }"><StatusTag :status="row.resultCode === 'WON' ? 3 : row.resultCode === 'LEADING' ? 3 : row.resultCode === 'OUTBID' ? 7 : row.resultCode === 'FAILED' ? 6 : 2" :text="row.resultText" /></template>
      </ElTableColumn>
      <ElTableColumn label="出价时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.bidTime) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" @click="goDetail(row.itemId)">查看详情</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <EmptyPanel v-else title="暂无竞拍记录" description="参与出价后，这里会展示你的竞拍流水、当前领先状态和中标结果。" />

    <ElPagination
      v-if="total > query.size"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      layout="total, prev, pager, next"
      :total="total"
      @current-change="loadMyBids"
    />
  </div>
</template>

<style scoped>
.bid-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.bid-summary article {
  padding: 16px 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 248, 235, 0.92), rgba(255, 255, 255, 0.98));
}

.bid-summary span {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.bid-summary strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--el-text-color-primary);
}

.item-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.item-cell img {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid var(--el-border-color-light);
  background: #fff;
}

.item-link {
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  font-weight: 600;
  color: var(--el-color-primary);
  cursor: pointer;
  text-align: left;
}

.item-link:hover {
  text-decoration: underline;
}

@media (max-width: 900px) {
  .bid-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .bid-summary {
    grid-template-columns: 1fr;
  }
}
</style>
