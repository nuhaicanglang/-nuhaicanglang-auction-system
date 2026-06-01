<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MetricCard from '@/components/business/MetricCard.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { getMyWallet, listMyWalletTransactions } from '@/api/wallet'
import type { Wallet, WalletTransaction } from '@/types/domain'
import { formatDateTime, formatMoney, normalizeRecords } from '@/utils/format'

const wallet = ref<Wallet>({})
const transactions = ref<WalletTransaction[]>([])

onMounted(async () => {
  wallet.value = await getMyWallet().catch(() => ({}))
  transactions.value = normalizeRecords(await listMyWalletTransactions({ page: 1, size: 30 }).catch(() => ({ records: [] })))
})
</script>

<template>
  <div class="page-stack">
    <div class="grid-3">
      <MetricCard label="可用余额" :value="formatMoney(wallet.balance)" />
      <MetricCard label="冻结金额" :value="formatMoney(wallet.frozenBalance)" tone="warning" />
      <MetricCard label="累计金额" :value="formatMoney(wallet.totalAmount)" tone="info" />
    </div>
    <ElTable v-if="transactions.length" :data="transactions" border>
      <ElTableColumn prop="transactionNo" label="流水号" min-width="180" />
      <ElTableColumn prop="actionType" label="类型" width="120" />
      <ElTableColumn label="金额" width="130"><template #default="{ row }">{{ formatMoney(row.amount) }}</template></ElTableColumn>
      <ElTableColumn prop="remark" label="备注" min-width="220" />
      <ElTableColumn label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
    </ElTable>
    <EmptyPanel v-else title="暂无资金流水" description="出价冻结、支付、退款或管理员调账后会生成流水。" />
  </div>
</template>
