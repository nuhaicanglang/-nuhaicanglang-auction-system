<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MetricCard from '@/components/business/MetricCard.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import { listMyItems } from '@/api/items'
import { listBuyerOrders, listSellerOrders } from '@/api/orders'
import { getMyCredit, getMyWallet } from '@/api/wallet'
import type { Credit, Wallet } from '@/types/domain'
import { formatMoney, normalizeTotal } from '@/utils/format'

const router = useRouter()
const wallet = ref<Wallet>({})
const credit = ref<Credit>({})
const itemCount = ref(0)
const buyerOrderCount = ref(0)
const sellerOrderCount = ref(0)

onMounted(async () => {
  const [walletData, creditData, itemPage, buyerPage, sellerPage] = await Promise.all([
    getMyWallet().catch(() => ({})),
    getMyCredit().catch(() => ({})),
    listMyItems({ page: 1, size: 1 }).catch(() => ({ records: [] })),
    listBuyerOrders({ page: 1, size: 1 }).catch(() => ({ records: [] })),
    listSellerOrders({ page: 1, size: 1 }).catch(() => ({ records: [] })),
  ])
  wallet.value = walletData
  credit.value = creditData
  itemCount.value = normalizeTotal(itemPage)
  buyerOrderCount.value = normalizeTotal(buyerPage)
  sellerOrderCount.value = normalizeTotal(sellerPage)
})
</script>

<template>
  <div class="page-stack">
    <SectionHeader
      title="用户中心"
      description="买家与卖家使用同一账号，根据业务场景进入不同工作流。"
    >
      <template #actions>
        <ElButton type="primary" @click="router.push('/user/publish')">发布拍品</ElButton>
      </template>
    </SectionHeader>

    <div class="grid-4">
      <MetricCard label="可用余额" :value="formatMoney(wallet.balance)" hint="出价保证金和支付从钱包扣减" />
      <MetricCard label="冻结金额" :value="formatMoney(wallet.frozenBalance)" hint="竞拍保证金冻结中" tone="warning" />
      <MetricCard label="信用分" :value="credit.score ?? '-'" :hint="credit.levelName || '初始 80 分'" tone="success" />
      <MetricCard label="我的拍品" :value="itemCount" hint="发布、编辑与下架管理" tone="info" />
    </div>

    <div class="quick-actions surface">
      <button type="button" @click="router.push('/user/items')">
        <strong>{{ itemCount }}</strong>
        <span>我的拍品</span>
      </button>
      <button type="button" @click="router.push('/user/orders/buyer')">
        <strong>{{ buyerOrderCount }}</strong>
        <span>买家订单</span>
      </button>
      <button type="button" @click="router.push('/user/orders/seller')">
        <strong>{{ sellerOrderCount }}</strong>
        <span>卖家订单</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 18px;
}

.quick-actions button {
  display: grid;
  min-height: 110px;
  place-items: center;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: #fffaf1;
  color: var(--color-ink);
  cursor: pointer;
}

.quick-actions strong {
  font-family: var(--font-mono);
  font-size: 32px;
}

.quick-actions span {
  color: var(--color-muted);
}

@media (max-width: 720px) {
  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
