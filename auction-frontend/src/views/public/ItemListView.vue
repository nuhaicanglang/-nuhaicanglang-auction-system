<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import AuctionCard from '@/components/business/AuctionCard.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import { listCategories, listItems } from '@/api/items'
import type { AuctionItem, Category } from '@/types/domain'
import { normalizeRecords, normalizeTotal } from '@/utils/format'

const route = useRoute()
const loading = ref(false)
const items = ref<AuctionItem[]>([])
const categories = ref<Category[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 12,
  keyword: String(route.query.keyword ?? ''),
  categoryId: route.query.categoryId ? String(route.query.categoryId) : '',
  status: '',
  priceMin: '',
  priceMax: '',
  sort: 'endingSoon',
})

async function loadItems() {
  loading.value = true
  try {
    const page = await listItems({
      ...query,
      categoryId: query.categoryId || undefined,
      status: query.status || undefined,
      priceMin: query.priceMin || undefined,
      priceMax: query.priceMax || undefined,
    })
    items.value = normalizeRecords(page)
    total.value = normalizeTotal(page)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories().catch(() => [])
  await loadItems()
})
</script>

<template>
  <div class="page-shell page-stack item-list-view">
    <SectionHeader
      kicker="Auction Hall"
      title="拍品大厅"
      description="按分类、价格、状态和关键词筛选拍品，快速找到合适的竞拍目标。"
    />

    <ElForm class="filter surface" :model="query" label-position="top" @submit.prevent="loadItems">
      <ElFormItem label="关键词">
        <ElInput v-model="query.keyword" placeholder="输入拍品标题或描述" clearable />
      </ElFormItem>
      <ElFormItem label="分类">
        <ElSelect v-model="query.categoryId" clearable placeholder="全部分类">
          <ElOption v-for="category in categories" :key="category.id" :label="category.name" :value="String(category.id)" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="状态">
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="待审核" value="1" />
          <ElOption label="待开拍" value="2" />
          <ElOption label="拍卖中" value="3" />
          <ElOption label="已成交" value="5" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="价格区间">
        <div class="price-filter">
          <ElInput v-model="query.priceMin" inputmode="decimal" placeholder="最低价" />
          <ElInput v-model="query.priceMax" inputmode="decimal" placeholder="最高价" />
        </div>
      </ElFormItem>
      <ElFormItem label="排序">
        <ElSelect v-model="query.sort">
          <ElOption label="即将结束" value="endingSoon" />
          <ElOption label="最新发布" value="latest" />
          <ElOption label="出价最多" value="bidCount" />
          <ElOption label="围观最多" value="viewCount" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem>
        <ElButton type="primary" :loading="loading" @click="loadItems">筛选</ElButton>
      </ElFormItem>
    </ElForm>

    <div class="result-bar">
      <span>共 {{ total }} 件拍品</span>
    </div>

    <div v-if="items.length" v-loading="loading" class="auction-grid">
      <AuctionCard v-for="item in items" :key="item.id" :item="item" />
    </div>
    <EmptyPanel v-else title="没有匹配的拍品" description="可以放宽分类、状态或价格区间后再试。" />
  </div>
</template>

<style scoped>
.item-list-view {
  padding: 30px 0 56px;
}

.filter {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1.4fr 1fr auto;
  gap: 14px;
  align-items: end;
  padding: 18px;
}

.price-filter {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.result-bar {
  color: var(--color-muted);
  font-weight: 700;
}

.auction-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1100px) {
  .filter,
  .auction-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .filter,
  .auction-grid {
    grid-template-columns: 1fr;
  }
}
</style>
