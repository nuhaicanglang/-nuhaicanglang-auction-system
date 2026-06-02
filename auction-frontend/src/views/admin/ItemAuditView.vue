<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { auditItem, listAuditItems, seedSampleItems } from '@/api/admin'
import type { AuctionItem } from '@/types/domain'
import { formatDateTime, formatMoney, normalizeRecords } from '@/utils/format'

const items = ref<AuctionItem[]>([])
const loading = ref(false)
const injecting = ref(false)
const sampleCount = ref(12)

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

async function injectSamples() {
  await ElMessageBox.confirm(
    `将以当前管理员身份直接发布 ${sampleCount.value} 个带图样例拍品，生成后会立即出现在前台列表中。`,
    '批量注入样例拍品',
    { type: 'warning' },
  )

  injecting.value = true
  try {
    const result = await seedSampleItems({ count: sampleCount.value })
    ElMessage.success(`已发布 ${result.count} 个样例拍品，可前往前台拍品列表查看`)
  } finally {
    injecting.value = false
  }
}

onMounted(loadItems)
</script>

<template>
  <div class="page-stack">
    <section class="seed-panel">
      <div>
        <p class="section-kicker">Sample Inventory</p>
        <h3>批量注入样例拍品</h3>
        <p class="muted">一次生成多分类、带图片、可直接展示和竞拍的样例商品，适合联调与演示。</p>
      </div>
      <div class="seed-actions">
        <span class="muted">数量</span>
        <ElInputNumber v-model="sampleCount" :min="1" :max="30" :step="1" />
        <ElButton type="primary" :loading="injecting" @click="injectSamples">发布样例商品</ElButton>
      </div>
    </section>

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

<style scoped>
.seed-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  background:
    linear-gradient(120deg, rgba(14, 55, 73, 0.92), rgba(29, 82, 92, 0.96)),
    radial-gradient(circle at top right, rgba(231, 187, 75, 0.24), transparent 38%);
  color: #fff7ea;
}

.seed-panel h3 {
  margin: 4px 0 8px;
  font-family: var(--font-display);
  font-size: 24px;
}

.seed-panel .muted {
  color: rgba(255, 247, 234, 0.78);
}

.seed-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .seed-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
