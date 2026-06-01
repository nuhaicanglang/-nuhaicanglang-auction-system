<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Clock3 } from '@lucide/vue'
import { formatDuration, secondsUntil } from '@/utils/format'

const props = defineProps<{
  endTime?: string
}>()

const nowTick = ref(Date.now())
const remaining = computed(() => secondsUntil(props.endTime, nowTick.value))

let timer: number | undefined

onMounted(() => {
  timer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <span class="countdown" :class="{ ended: remaining <= 0 }">
    <Clock3 aria-hidden="true" />
    {{ remaining > 0 ? formatDuration(remaining) : '已结束' }}
  </span>
</template>

<style scoped>
.countdown {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  gap: 7px;
  padding: 5px 10px;
  border: 1px solid rgba(183, 128, 44, 0.34);
  border-radius: 999px;
  color: var(--color-primary-dark);
  background: rgba(183, 128, 44, 0.1);
  font-family: var(--font-mono);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.countdown svg {
  width: 15px;
  height: 15px;
}

.countdown.ended {
  color: var(--color-muted);
  border-color: var(--color-line);
  background: #f7f1e7;
}
</style>
