<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { auditItem, listAuditItems } from '@/api/admin'
import type { AuctionItem } from '@/types/domain'
import { formatDateTime, formatMoney, normalizeRecords } from '@/utils/format'

const items = ref<AuctionItem[]>([])
const loading = ref(false)

async function loadItems() {
  loading.value = true
  try {
    items.value = normalizeRecords(await listAuditItems({ page: 1, size: 50 }))
  } finally {
    loading.value = false
  }
}

async function audit(id: string | number, action: 'PASS' | 'REJECT') {
  const { value } = await ElMessageBox.prompt('请输入审核备注', action === 'PASS' ? '审核通过' : '驳回拍品', {
    inputValue: action === 'PASS' ? '审核通过' : '请完善拍品信息后重新提交',
  })
  await auditItem(id, { action, remark: value })
  ElMessage.success(action === 'PASS' ? '拍品已通过审核' : '拍品已驳回')
  await loadItems()
}

onMounted(loadItems)
</script>

<template>
  <div class="page-stack">
    <ElTable v-if="items.length" v-loading="loading" :data="items" border>
      <ElTableColumn label="拍品" min-width="260">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <p class="muted">{{ row.subtitle || row.categoryPath }}</p>
        </template>
      </ElTableColumn>
      <ElTableColumn label="起拍价" width="130"><template #default="{ row }">{{ formatMoney(row.startPrice) }}</template></ElTableColumn>
      <ElTableColumn label="保证金" width="130"><template #default="{ row }">{{ formatMoney(row.deposit) }}</template></ElTableColumn>
      <ElTableColumn label="状态" width="120"><template #default="{ row }"><StatusTag :status="row.status" /></template></ElTableColumn>
      <ElTableColumn label="提交时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
      <ElTableColumn label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" type="primary" @click="audit(row.id, 'PASS')">通过</ElButton>
          <ElButton size="small" type="danger" plain @click="audit(row.id, 'REJECT')">驳回</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    <EmptyPanel v-else title="没有待审核拍品" description="卖家提交拍品后会进入这里等待审核。" />
  </div>
</template>
