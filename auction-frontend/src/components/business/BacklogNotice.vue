<script setup lang="ts">
defineProps<{
  title: string
  endpoint: string
  description: string
}>()
</script>

<template>
  <div class="backlog surface">
    <p class="section-kicker">接口待办</p>
    <h1>{{ title }}</h1>
    <p>{{ description }}</p>
    <code>{{ endpoint }}</code>
  </div>
</template>

<style scoped>
.backlog {
  display: grid;
  gap: 12px;
  padding: 28px;
}

h1 {
  margin: 0;
  font-family: var(--font-display);
}

p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

code {
  width: fit-content;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: #f6ead7;
  color: var(--color-primary-dark);
  font-family: var(--font-mono);
}
</style>
