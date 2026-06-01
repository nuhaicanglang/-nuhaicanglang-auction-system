<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BacklogNotice from '@/components/business/BacklogNotice.vue'

const route = useRoute()

const mapping = computed(() => {
  const title = String(route.meta.title || '接口待办')
  const table: Record<string, { endpoint: string; description: string }> = {
    我的收藏: {
      endpoint: 'GET /api/me/favorites, POST /api/items/{id}/favorite',
      description: '后端当前未公开收藏控制器，前端先保留标准入口和空状态，后续补齐后可直接接入。',
    },
    站内信: {
      endpoint: 'GET /api/me/notifications, PUT /api/me/notifications/{id}/read',
      description: '后端已有通知实体和 WebSocket 推送，但缺少站内信列表/已读接口。',
    },
    订单总表: {
      endpoint: 'GET /api/admin/orders',
      description: '当前只有买家/卖家订单接口，后台全量订单总表可作为后续运营接口补充。',
    },
    操作日志: {
      endpoint: 'GET /api/admin/logs',
      description: '后端已有 Log 注解，前端预留审计日志入口，等待日志查询接口落地。',
    },
  }
  return {
    title,
    ...(table[title] ?? {
      endpoint: '待设计',
      description: '该模块属于企业级扩展能力，前端已预留页面入口。',
    }),
  }
})
</script>

<template>
  <BacklogNotice :title="mapping.title" :endpoint="mapping.endpoint" :description="mapping.description" />
</template>
