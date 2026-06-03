<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Decimal from 'decimal.js'
import type { AuctionItem } from '@/types/domain'
import { buyNow, placeBid } from '@/api/items'
import PriceText from '@/components/common/PriceText.vue'
import { formatMoney, getAuctionDisplayStatus, toDecimal } from '@/utils/format'

const props = defineProps<{
  item: AuctionItem
  authenticated: boolean
}>()

const emit = defineEmits<{
  success: []
}>()

const submitting = ref(false)
const bidPrice = ref('')

const displayStatus = computed(() => getAuctionDisplayStatus(props.item))
const canBid = computed(() => Number(displayStatus.value.status) === 3)
const statusHint = computed(() => displayStatus.value.text || '当前状态不可出价')
const minBid = computed(() =>
  toDecimal(props.item.currentPrice ?? props.item.startPrice).plus(toDecimal(props.item.bidIncrement || 1)),
)

function fillMinBid() {
  bidPrice.value = minBid.value.toFixed(2)
}

async function submitBid() {
  if (!canBid.value) {
    ElMessage.warning(statusHint.value)
    return
  }
  if (!props.authenticated) {
    ElMessage.warning('请先登录后再参与竞拍')
    return
  }
  const value = new Decimal(bidPrice.value || 0)
  if (value.lessThan(minBid.value)) {
    ElMessage.warning(`最低有效出价为 ${formatMoney(minBid.value.toFixed(2))}`)
    return
  }
  submitting.value = true
  try {
    await placeBid(props.item.id, value.toFixed(2))
    ElMessage.success('出价成功，竞拍状态已刷新')
    bidPrice.value = ''
    emit('success')
  } finally {
    submitting.value = false
  }
}

async function submitBuyNow() {
  if (!canBid.value) {
    ElMessage.warning(statusHint.value)
    return
  }
  if (!props.authenticated) {
    ElMessage.warning('请先登录后再一口价购买')
    return
  }
  submitting.value = true
  try {
    await buyNow(props.item.id)
    ElMessage.success('一口价成交，订单已生成')
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <aside class="bid-panel surface">
    <div>
      <p class="panel-label">当前最高价</p>
      <PriceText :value="item.currentPrice ?? item.startPrice" strong />
    </div>

    <div class="rule-grid">
      <span>加价幅度 <strong>{{ formatMoney(item.bidIncrement) }}</strong></span>
      <span>保证金 <strong>{{ formatMoney(item.deposit) }}</strong></span>
      <span v-if="item.buyNowPrice">一口价 <strong>{{ formatMoney(item.buyNowPrice) }}</strong></span>
    </div>

    <ElForm v-if="canBid" label-position="top">
      <ElFormItem label="我的出价">
        <ElInput v-model="bidPrice" inputmode="decimal" placeholder="请输入出价金额">
          <template #append>
            <ElButton @click="fillMinBid">最低价</ElButton>
          </template>
        </ElInput>
      </ElFormItem>
      <div class="bid-actions">
        <ElButton type="primary" :loading="submitting" @click="submitBid">确认出价</ElButton>
        <ElButton v-if="item.buyNowPrice" :loading="submitting" @click="submitBuyNow">
          一口价成交
        </ElButton>
      </div>
    </ElForm>
    <ElAlert
      v-else
      type="info"
      :closable="false"
      show-icon
      :title="statusHint"
      description="该拍品当前不可继续出价，可前往订单中心查看后续交易状态。"
    />
  </aside>
</template>

<style scoped>
.bid-panel {
  display: grid;
  gap: 18px;
  padding: 20px;
}

.panel-label {
  margin: 0 0 8px;
  color: var(--color-muted);
  font-weight: 700;
}

.rule-grid {
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: #f8f1e6;
  color: var(--color-muted);
  font-size: 14px;
}

.rule-grid strong {
  color: var(--color-ink);
  font-family: var(--font-mono);
}

.bid-actions {
  display: grid;
  gap: 10px;
}
</style>
