<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import ChartPanel from '@/components/business/ChartPanel.vue'
import MetricCard from '@/components/business/MetricCard.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { getHotCategories, getStatsOverview, getStatsTrend, getTopItems } from '@/api/admin'
import type { StatsOverview, StatsTrendPoint, TopItem } from '@/types/domain'
import { formatMoney } from '@/utils/format'

const overview = ref<StatsOverview>({})
const trend = ref<StatsTrendPoint[]>([])
const topItems = ref<TopItem[]>([])
const categories = ref<Array<{ categoryId: string | number; categoryName: string; itemCount?: number }>>([])

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['新增用户', '新增拍品', '新增订单'] },
  grid: { left: 38, right: 18, top: 48, bottom: 36 },
  xAxis: { type: 'category', data: trend.value.map((item) => item.date) },
  yAxis: { type: 'value' },
  series: [
    { name: '新增用户', type: 'line', smooth: true, data: trend.value.map((item) => item.userCount ?? 0) },
    { name: '新增拍品', type: 'line', smooth: true, data: trend.value.map((item) => item.itemCount ?? 0) },
    { name: '新增订单', type: 'bar', data: trend.value.map((item) => item.orderCount ?? 0) },
  ],
}))

const categoryOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['45%', '72%'],
      data: categories.value.map((item) => ({ name: item.categoryName, value: item.itemCount ?? 0 })),
    },
  ],
}))

onMounted(async () => {
  const [overviewData, trendData, topData, categoryData] = await Promise.all([
    getStatsOverview().catch(() => ({})),
    getStatsTrend(30).catch(() => []),
    getTopItems(10).catch(() => []),
    getHotCategories(10).catch(() => []),
  ])
  overview.value = overviewData
  trend.value = trendData
  topItems.value = topData
  categories.value = categoryData
})
</script>

<template>
  <div class="page-stack">
    <div class="grid-4">
      <MetricCard label="今日新增用户" :value="overview.todayUserCount ?? 0" />
      <MetricCard label="今日新增拍品" :value="overview.todayItemCount ?? 0" tone="info" />
      <MetricCard label="今日订单" :value="overview.todayOrderCount ?? 0" tone="success" />
      <MetricCard label="今日成交额" :value="formatMoney(overview.todayDealAmount)" tone="warning" />
    </div>
    <div class="grid-2">
      <ChartPanel title="30 日运营趋势" :option="trendOption" />
      <ChartPanel title="热门分类分布" :option="categoryOption" />
    </div>
    <ElTable v-if="topItems.length" :data="topItems" border>
      <ElTableColumn prop="title" label="热门拍品" min-width="260" />
      <ElTableColumn label="当前价" width="140"><template #default="{ row }">{{ formatMoney(row.currentPrice) }}</template></ElTableColumn>
      <ElTableColumn prop="bidCount" label="出价数" width="100" />
      <ElTableColumn prop="viewCount" label="围观数" width="100" />
    </ElTable>
    <EmptyPanel v-else title="暂无热门拍品数据" description="完成交易或产生浏览出价后，榜单会自动生成。" />
  </div>
</template>
