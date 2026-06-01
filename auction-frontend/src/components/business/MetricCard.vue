<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  hint?: string
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}>()
</script>

<template>
  <div class="metric-card surface" :class="tone ?? 'primary'">
    <p>{{ label }}</p>
    <strong>{{ value }}</strong>
    <span v-if="hint">{{ hint }}</span>
  </div>
</template>

<style scoped>
.metric-card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-left: 4px solid var(--color-primary);
}

.metric-card p,
.metric-card span {
  margin: 0;
  color: var(--color-muted);
}

.metric-card strong {
  font-family: var(--font-mono);
  font-size: 28px;
  color: var(--color-ink);
}

.success {
  border-left-color: var(--color-success);
}

.warning {
  border-left-color: var(--color-warning);
}

.danger {
  border-left-color: var(--color-danger);
}

.info {
  border-left-color: var(--color-info);
}
</style>
