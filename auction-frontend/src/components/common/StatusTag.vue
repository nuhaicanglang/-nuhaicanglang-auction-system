<script setup lang="ts">
import { computed } from 'vue'
import { statusType } from '@/utils/format'

const props = defineProps<{
  status?: number | string
  text?: string
}>()

const label = computed(() => {
  if (props.text) {
    return props.text
  }
  const map: Record<string, string> = {
    '0': '禁用',
    '1': '待审核',
    '2': '待开拍',
    '3': '拍卖中',
    '4': '已结束',
    '5': '已成交',
    '6': '流拍',
    '7': '已下架',
  }
  return map[String(props.status ?? '')] ?? String(props.status ?? '未知')
})
</script>

<template>
  <ElTag :type="statusType(status)" effect="light">{{ label }}</ElTag>
</template>
