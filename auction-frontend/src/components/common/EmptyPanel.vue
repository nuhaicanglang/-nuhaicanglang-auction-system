<script setup lang="ts">
import { Inbox } from '@lucide/vue'

defineProps<{
  title?: string
  description?: string
}>()
</script>

<template>
  <div class="empty-panel surface">
    <Inbox class="empty-icon" aria-hidden="true" />
    <h2>{{ title ?? '暂无数据' }}</h2>
    <p>{{ description ?? '当前条件下没有可展示的记录。' }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<style scoped>
.empty-panel {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 44px 24px;
  text-align: center;
}

.empty-icon {
  width: 38px;
  height: 38px;
  color: var(--color-primary);
}

h2 {
  margin: 0;
  font-size: 20px;
}

p {
  max-width: 520px;
  margin: 0;
  color: var(--color-muted);
  line-height: 1.6;
}

.empty-action {
  margin-top: 10px;
}
</style>
