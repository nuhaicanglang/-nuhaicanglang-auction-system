<script setup lang="ts">
defineProps<{
  kicker?: string
  title: string
  description?: string
}>()
</script>

<template>
  <header class="section-header">
    <div>
      <p v-if="kicker" class="section-kicker">{{ kicker }}</p>
      <h1 class="section-title">{{ title }}</h1>
      <p v-if="description" class="section-description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="section-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.section-description {
  max-width: 720px;
  margin: 10px 0 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 720px) {
  .section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .section-actions {
    justify-content: flex-start;
  }
}
</style>
