<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney } from '@/utils/format'

const props = defineProps<{
  value?: string | number | null
  label?: string
  strong?: boolean
}>()

const text = computed(() => formatMoney(props.value))
</script>

<template>
  <span class="price-text" :class="{ strong }">
    <span v-if="label" class="price-label">{{ label }}</span>
    <span class="money">{{ text }}</span>
  </span>
</template>

<style scoped>
.price-text {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
}

.price-label {
  color: var(--color-muted);
  font-size: 13px;
}

.strong .money {
  font-size: 28px;
  font-weight: 800;
}
</style>
