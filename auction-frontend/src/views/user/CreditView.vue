<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MetricCard from '@/components/business/MetricCard.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { getMyCredit, listMyCreditLogs } from '@/api/wallet'
import type { Credit, CreditLog } from '@/types/domain'
import { formatDateTime, normalizeRecords } from '@/utils/format'

const credit = ref<Credit>({})
const logs = ref<CreditLog[]>([])

onMounted(async () => {
  credit.value = await getMyCredit().catch(() => ({}))
  logs.value = normalizeRecords(await listMyCreditLogs({ page: 1, size: 30 }).catch(() => ({ records: [] })))
})
</script>

<template>
  <div class="page-stack">
    <div class="grid-3">
      <MetricCard label="当前信用分" :value="credit.score ?? '-'" :hint="credit.levelName" tone="success" />
      <MetricCard label="信用状态" :value="credit.status ?? '-'" hint="状态由后端信用规则计算" tone="info" />
      <MetricCard label="最近变动" :value="formatDateTime(credit.lastEventAt)" tone="warning" />
    </div>
    <ElTable v-if="logs.length" :data="logs" border>
      <ElTableColumn prop="eventType" label="事件" width="160" />
      <ElTableColumn prop="deltaScore" label="变动" width="100" />
      <ElTableColumn prop="scoreBefore" label="变动前" width="100" />
      <ElTableColumn prop="scoreAfter" label="变动后" width="100" />
      <ElTableColumn prop="remark" label="备注" min-width="220" />
      <ElTableColumn label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
    </ElTable>
    <EmptyPanel v-else title="暂无信用记录" description="完成交易、评价或违规处理后会产生信用分流水。" />
  </div>
</template>
