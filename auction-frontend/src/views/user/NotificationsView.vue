<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { listNotifications, markNotificationRead } from '@/api/notifications'
import type { Notification } from '@/types/domain'
import { formatDateTime, normalizeRecords, normalizeTotal } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const notifications = ref<Notification[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 20,
  isRead: '',
})

async function loadNotifications() {
  loading.value = true
  try {
    const page = await listNotifications({
      ...query,
      isRead: query.isRead === '' ? undefined : Number(query.isRead),
    })
    notifications.value = normalizeRecords(page)
    total.value = normalizeTotal(page)
  } finally {
    loading.value = false
  }
}

async function read(item: Notification) {
  if (Number(item.isRead) !== 1) {
    await markNotificationRead(item.id)
    await loadNotifications()
  }
  if (item.relatedItemId) {
    router.push(`/items/${item.relatedItemId}`)
  }
}

function search() {
  query.page = 1
  loadNotifications()
}

onMounted(loadNotifications)
</script>

<template>
  <div class="page-stack">
    <section class="toolbar">
      <ElSelect v-model="query.isRead" placeholder="读取状态" @change="search">
        <ElOption label="全部消息" value="" />
        <ElOption label="未读" value="0" />
        <ElOption label="已读" value="1" />
      </ElSelect>
    </section>

    <ElTable v-if="notifications.length" v-loading="loading" :data="notifications" border>
      <ElTableColumn label="状态" width="90">
        <template #default="{ row }">
          <ElTag :type="Number(row.isRead) === 1 ? 'info' : 'warning'">{{ Number(row.isRead) === 1 ? '已读' : '未读' }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="title" label="标题" min-width="180" />
      <ElTableColumn prop="content" label="内容" min-width="280" show-overflow-tooltip />
      <ElTableColumn label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
      <ElTableColumn label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" type="primary" plain @click="read(row)">{{ Number(row.isRead) === 1 ? '查看' : '已读并查看' }}</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    <EmptyPanel v-else title="暂无站内信" description="出价被超、竞拍中标等通知会出现在这里。" />

    <ElPagination
      v-if="total > query.size"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      layout="total, prev, pager, next"
      :total="total"
      @current-change="loadNotifications"
    />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
}
</style>
