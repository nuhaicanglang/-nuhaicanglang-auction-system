<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { completeOrder, createReview, listBuyerOrders, listSellerOrders, payOrder, shipOrder } from '@/api/orders'
import type { Order } from '@/types/domain'
import { formatDateTime, formatMoney, normalizeRecords } from '@/utils/format'

const route = useRoute()
const orders = ref<Order[]>([])
const loading = ref(false)
const isSeller = computed(() => route.path.includes('/seller'))

async function loadOrders() {
  loading.value = true
  try {
    orders.value = normalizeRecords(await (isSeller.value ? listSellerOrders() : listBuyerOrders()))
  } finally {
    loading.value = false
  }
}

async function pay(id: string | number) {
  await payOrder(id)
  ElMessage.success('支付成功')
  await loadOrders()
}

async function ship(id: string | number) {
  await ElMessageBox.prompt('请输入物流单号或发货备注', '确认发货')
  await shipOrder(id)
  ElMessage.success('发货成功')
  await loadOrders()
}

async function complete(id: string | number) {
  await completeOrder(id)
  ElMessage.success('订单已完成')
  await loadOrders()
}

async function review(id: string | number) {
  await createReview(id, { score: 5, content: '交易顺利，体验良好。' })
  ElMessage.success('评价已提交')
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-stack">
    <ElTable v-if="orders.length" v-loading="loading" :data="orders" border>
      <ElTableColumn label="订单" min-width="260">
        <template #default="{ row }">
          <strong>{{ row.itemTitle }}</strong>
          <p class="muted">{{ row.orderNo }}</p>
        </template>
      </ElTableColumn>
      <ElTableColumn label="成交价" width="130">
        <template #default="{ row }">{{ formatMoney(row.dealPrice) }}</template>
      </ElTableColumn>
      <ElTableColumn label="应付" width="130">
        <template #default="{ row }">{{ formatMoney(row.payAmount) }}</template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="120">
        <template #default="{ row }"><StatusTag :status="row.status" :text="row.statusText" /></template>
      </ElTableColumn>
      <ElTableColumn label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <ElButton v-if="!isSeller" size="small" type="primary" @click="pay(row.id)">支付</ElButton>
          <ElButton v-if="isSeller" size="small" type="primary" @click="ship(row.id)">发货</ElButton>
          <ElButton v-if="!isSeller" size="small" @click="complete(row.id)">确认收货</ElButton>
          <ElButton size="small" @click="review(row.id)">评价</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    <EmptyPanel v-else title="暂无订单" description="竞拍成交后会自动生成待支付订单。" />
  </div>
</template>
