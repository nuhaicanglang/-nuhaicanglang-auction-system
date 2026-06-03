<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AuctionCard from '@/components/business/AuctionCard.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import { searchItems, suggestItems } from '@/api/search'
import type { AuctionItem, Facet } from '@/types/domain'

const route = useRoute()
const loading = ref(false)
const suggestions = ref<AuctionItem[]>([])
const items = ref<AuctionItem[]>([])
const categoryFacets = ref<Facet[]>([])
const statusFacets = ref<Facet[]>([])

const query = reactive({
  keyword: String(route.query.keyword ?? ''),
  page: 1,
  size: 12,
  sort: 'relevance',
  order: 'desc',
  saveHistory: true,
})

async function loadSearch() {
  loading.value = true
  try {
    const result = await searchItems(query)
    items.value = result.items ?? []
    categoryFacets.value = result.categoryFacets ?? []
    statusFacets.value = result.statusFacets ?? []
  } finally {
    loading.value = false
  }
}

watch(
  () => query.keyword,
  async (keyword) => {
    if (!keyword) {
      suggestions.value = []
      return
    }
    suggestions.value = await suggestItems(keyword).catch(() => [])
  },
)

onMounted(loadSearch)
</script>

<template>
  <div class="page-shell page-stack search-view">
    <SectionHeader
      kicker="Search"
      title="全文搜索"
      description="支持关键词搜索、联想推荐与分类聚合，帮助你更快找到目标拍品。"
    />

    <section class="search-panel surface">
      <ElInput v-model="query.keyword" size="large" placeholder="搜索国画 山水、劳力士、翡翠手镯..." clearable />
      <ElSelect v-model="query.sort" size="large">
        <ElOption label="相关度" value="relevance" />
        <ElOption label="即将结束" value="endTime" />
        <ElOption label="当前价格" value="currentPrice" />
        <ElOption label="出价数" value="bidCount" />
      </ElSelect>
      <ElButton type="primary" size="large" :loading="loading" @click="loadSearch">搜索</ElButton>
    </section>

    <div v-if="suggestions.length" class="suggestions">
      <ElTag v-for="item in suggestions" :key="item.id" effect="plain" @click="query.keyword = item.title">
        {{ item.title }}
      </ElTag>
    </div>

    <div class="facet-row">
      <span v-for="facet in categoryFacets" :key="`c-${facet.key}`">{{ facet.key }} {{ facet.count }}</span>
      <span v-for="facet in statusFacets" :key="`s-${facet.key}`">状态 {{ facet.key }} {{ facet.count }}</span>
    </div>

    <div v-if="items.length" v-loading="loading" class="auction-grid">
      <AuctionCard v-for="item in items" :key="item.id" :item="item" />
    </div>
    <EmptyPanel v-else title="没有搜索结果" description="换个关键词试试，也可以放宽筛选条件后重新搜索。" />
  </div>
</template>

<style scoped>
.search-view {
  padding: 30px 0 56px;
}

.search-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px auto;
  gap: 12px;
  padding: 18px;
}

.suggestions,
.facet-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestions .el-tag {
  cursor: pointer;
}

.facet-row span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #fffaf1;
  color: var(--color-muted);
  font-size: 13px;
}

.auction-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 900px) {
  .search-panel,
  .auction-grid {
    grid-template-columns: 1fr;
  }
}
</style>
