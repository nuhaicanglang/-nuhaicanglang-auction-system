<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { clearSearchHistory, getSearchHistory } from '@/api/search'

const router = useRouter()
const history = ref<string[]>([])

async function loadHistory() {
  history.value = await getSearchHistory().catch(() => [])
}

async function clearAll() {
  await clearSearchHistory()
  ElMessage.success('搜索历史已清空')
  await loadHistory()
}

onMounted(loadHistory)
</script>

<template>
  <div class="page-stack">
    <div class="toolbar">
      <ElButton :disabled="!history.length" @click="clearAll">清空历史</ElButton>
    </div>
    <div v-if="history.length" class="history-list surface">
      <button
        v-for="keyword in history"
        :key="keyword"
        type="button"
        @click="router.push({ path: '/search', query: { keyword } })"
      >
        {{ keyword }}
      </button>
    </div>
    <EmptyPanel v-else title="暂无搜索历史" description="登录后搜索拍品会记录最近关键词。" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 18px;
}

.history-list button {
  min-height: 40px;
  padding: 8px 12px;
  border: 1px solid var(--color-line);
  border-radius: 999px;
  background: #fffaf1;
  color: var(--color-ink);
  cursor: pointer;
}
</style>
