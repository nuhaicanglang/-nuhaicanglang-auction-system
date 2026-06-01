<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportApi } from '@/api/admin'

const loadingKey = ref('')

async function download(type: 'orders' | 'users' | 'wallet') {
  loadingKey.value = type
  try {
    await exportApi[type]()
    ElMessage.success('导出任务已完成')
  } finally {
    loadingKey.value = ''
  }
}
</script>

<template>
  <div class="export-grid">
    <ElCard>
      <template #header>订单导出</template>
      <p>导出订单、拍品、买卖双方、成交价和状态。</p>
      <ElButton type="primary" :loading="loadingKey === 'orders'" @click="download('orders')">下载订单 Excel</ElButton>
    </ElCard>
    <ElCard>
      <template #header>用户导出</template>
      <p>导出用户基础信息、状态和注册时间。</p>
      <ElButton type="primary" :loading="loadingKey === 'users'" @click="download('users')">下载用户 Excel</ElButton>
    </ElCard>
    <ElCard>
      <template #header>钱包流水导出</template>
      <p>导出全平台资金流水，用于财务审计。</p>
      <ElButton type="primary" :loading="loadingKey === 'wallet'" @click="download('wallet')">下载钱包 Excel</ElButton>
    </ElCard>
  </div>
</template>

<style scoped>
.export-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

p {
  min-height: 56px;
  color: var(--color-muted);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .export-grid {
    grid-template-columns: 1fr;
  }
}
</style>
