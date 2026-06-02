<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listOperLogs } from '@/api/admin'
import type { OperLog } from '@/types/domain'
import { formatDateTime, normalizeRecords, normalizeTotal, safeText } from '@/utils/format'

const loading = ref(false)
const logs = ref<OperLog[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  module: '',
  businessType: '',
  status: '',
})

async function loadLogs() {
  loading.value = true
  try {
    const page = await listOperLogs({
      ...query,
      keyword: query.keyword || undefined,
      module: query.module || undefined,
      businessType: query.businessType || undefined,
      status: query.status || undefined,
    })
    logs.value = normalizeRecords(page)
    total.value = normalizeTotal(page)
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadLogs()
}

onMounted(loadLogs)
</script>

<template>
  <div class="page-stack">
    <section class="table-toolbar surface">
      <ElInput v-model="query.keyword" placeholder="描述 / URL / TraceId" clearable />
      <ElInput v-model="query.module" placeholder="模块" clearable />
      <ElSelect v-model="query.status" placeholder="状态" clearable>
        <ElOption label="成功" :value="0" />
        <ElOption label="失败" :value="1" />
      </ElSelect>
      <ElButton type="primary" @click="search">查询</ElButton>
    </section>

    <ElTable v-loading="loading" :data="logs" border>
      <ElTableColumn prop="module" label="模块" width="130" />
      <ElTableColumn prop="businessType" label="类型" width="110" />
      <ElTableColumn prop="description" label="描述" min-width="180" />
      <ElTableColumn prop="requestMethod" label="方法" width="90" />
      <ElTableColumn prop="requestUrl" label="路径" min-width="220" show-overflow-tooltip />
      <ElTableColumn label="操作人" width="130">
        <template #default="{ row }">{{ safeText(row.operUserName || row.operUserId) }}</template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="90">
        <template #default="{ row }">
          <ElTag :type="Number(row.status) === 0 ? 'success' : 'danger'">{{ Number(row.status) === 0 ? '成功' : '失败' }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="耗时" width="90"><template #default="{ row }">{{ row.costMs ?? '-' }}ms</template></ElTableColumn>
      <ElTableColumn label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
    </ElTable>

    <ElPagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50]"
      :total="total"
      @current-change="loadLogs"
      @size-change="search"
    />
  </div>
</template>

<style scoped>
.table-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 140px auto;
  gap: 12px;
  padding: 16px;
}

@media (max-width: 900px) {
  .table-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
