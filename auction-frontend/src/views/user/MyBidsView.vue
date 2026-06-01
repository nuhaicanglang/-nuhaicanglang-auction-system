<template>
  <BacklogNotice
    title="我的竞拍"
    endpoint="GET /api/me/bids"
    description="后端当前只有商品维度出价记录 `/api/items/{itemId}/bids`，缺少用户维度竞拍汇总。前端已预留进行中、已中标、已流拍和全部流水的入口。"
  />
</template>

<script setup lang="ts">
import BacklogNotice from '@/components/business/BacklogNotice.vue'
</script>
