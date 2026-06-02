<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { listAdminOrders } from '@/api/admin'
import type { Order } from '@/types/domain'
import { formatDateTime, formatMoney, normalizeRecords, normalizeTotal } from '@/utils/format'

const loading = ref(false)
const orders = ref<Order[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 20,
  status: '',
  keyword: '',
  orderNo: '',
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待支付', value: 1 },
  { label: '已支付', value: 2 },
  { label: '已发货', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 },
  { label: '已关闭', value: 6 },
]

const statusText: Record<number, string> = {
  1: '待支付',
  2: '已支付',
  3: '已发货',
  4: '已完成',
  5: '已取消',
  6: '已关闭',
}

async function loadOrders() {
  loading.value = true
  try {
    const params = {
      ...query,
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      orderNo: query.orderNo || undefined,
    }
    const page = await listAdminOrders(params)
    orders.value = normalizeRecords(page)
    total.value = normalizeTotal(page)
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadOrders()
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-stack">
    <section class="table-toolbar surface">
      <ElInput v-model="query.orderNo" placeholder="订单号" clearable />
      <ElInput v-model="query.keyword" placeholder="拍品标题" clearable />
      <ElSelect v-model="query.status" placeholder="状态">
        <ElOption v-for="option in statusOptions" :key="option.label" :label="option.label" :value="option.value" />
      </ElSelect>
      <ElButton type="primary" @click="search">查询</ElButton>
    </section>

    <ElTable v-loading="loading" :data="orders" border>
      <ElTableColumn prop="orderNo" label="订单号" min-width="210" />
      <ElTableColumn prop="itemTitle" label="拍品" min-width="240" />
      <ElTableColumn prop="buyerId" label="买家" width="120" />
      <ElTableColumn prop="sellerId" label="卖家" width="120" />
      <ElTableColumn label="成交价" width="130"><template #default="{ row }">{{ formatMoney(row.dealPrice) }}</template></ElTableColumn>
      <ElTableColumn label="应付" width="130"><template #default="{ row }">{{ formatMoney(row.payAmount) }}</template></ElTableColumn>
      <ElTableColumn label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.status" :text="statusText[Number(row.status)] || row.statusText" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="创建时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
    </ElTable>

    <ElPagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50]"
      :total="total"
      @current-change="loadOrders"
      @size-change="search"
    />
  </div>
</template>

<style scoped>
.table-toolbar {
  display: grid;
  grid-template-columns: 220px 1fr 180px auto;
  gap: 12px;
  padding: 16px;
}

@media (max-width: 900px) {
  .table-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
