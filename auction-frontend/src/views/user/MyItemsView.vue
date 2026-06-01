<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { listMyItems, offlineItem } from '@/api/items'
import type { AuctionItem } from '@/types/domain'
import { formatDateTime, formatMoney, normalizeRecords } from '@/utils/format'

const router = useRouter()
const items = ref<AuctionItem[]>([])
const loading = ref(false)

async function loadItems() {
  loading.value = true
  try {
    items.value = normalizeRecords(await listMyItems({ page: 1, size: 50 }))
  } finally {
    loading.value = false
  }
}

async function offline(id: string | number) {
  await ElMessageBox.confirm('确认下架该拍品？', '下架确认', { type: 'warning' })
  await offlineItem(id)
  ElMessage.success('拍品已下架')
  await loadItems()
}

onMounted(loadItems)
</script>

<template>
  <div class="page-stack">
    <div class="toolbar">
      <ElButton type="primary" @click="router.push('/user/publish')">发布拍品</ElButton>
    </div>
    <ElTable v-if="items.length" v-loading="loading" :data="items" border>
      <ElTableColumn label="拍品" min-width="260">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <p class="muted">{{ row.categoryPath || row.subtitle }}</p>
        </template>
      </ElTableColumn>
      <ElTableColumn label="当前价" width="130">
        <template #default="{ row }">{{ formatMoney(row.currentPrice ?? row.startPrice) }}</template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="120">
        <template #default="{ row }"><StatusTag :status="row.status" :text="row.statusText" /></template>
      </ElTableColumn>
      <ElTableColumn label="结束时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.actualEndTime || row.endTime) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" @click="router.push(`/items/${row.id}`)">查看</ElButton>
          <ElButton size="small" @click="router.push(`/user/items/${row.id}/edit`)">编辑</ElButton>
          <ElButton size="small" type="danger" plain @click="offline(row.id)">下架</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    <EmptyPanel v-else title="还没有发布拍品">
      <template #action>
        <ElButton type="primary" @click="router.push('/user/publish')">发布第一件拍品</ElButton>
      </template>
    </EmptyPanel>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
}
</style>
