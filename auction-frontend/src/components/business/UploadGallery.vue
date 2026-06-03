<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@lucide/vue'
import { uploadImage } from '@/api/upload'
import { resolveAssetUrl } from '@/utils/assets'

const model = defineModel<string[]>({ default: [] })

const props = defineProps<{
  max?: number
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

async function onFilesChanged(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  const limit = props.max ?? 9
  if (model.value.length + files.length > limit) {
    ElMessage.warning(`最多上传 ${limit} 张图片`)
    input.value = ''
    return
  }

  uploading.value = true
  try {
    for (const file of files) {
      const url = await uploadImage(file)
      model.value = [...model.value, url]
    }
    ElMessage.success('图片上传成功')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function removeAt(index: number) {
  model.value = model.value.filter((_, current) => current !== index)
}
</script>

<template>
  <div class="upload-gallery">
    <div class="gallery-grid">
      <div v-for="(url, index) in model" :key="url" class="image-tile">
        <img :src="resolveAssetUrl(url)" alt="拍品图片" />
        <ElButton size="small" type="danger" plain @click="removeAt(index)">移除</ElButton>
      </div>
      <button class="upload-tile" type="button" :disabled="uploading" @click="fileInput?.click()">
        <Upload aria-hidden="true" />
        <span>{{ uploading ? '上传中...' : '上传图片' }}</span>
      </button>
    </div>
    <input
      ref="fileInput"
      class="sr-only"
      type="file"
      accept="image/png,image/jpeg,image/gif,image/webp"
      multiple
      @change="onFilesChanged"
    />
  </div>
</template>

<style scoped>
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(124px, 1fr));
  gap: 12px;
}

.image-tile,
.upload-tile {
  min-height: 128px;
  border: 1px dashed var(--color-line);
  border-radius: var(--radius-md);
  background: #fffaf1;
}

.image-tile {
  position: relative;
  overflow: hidden;
}

.image-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-tile .el-button {
  position: absolute;
  right: 8px;
  bottom: 8px;
}

.upload-tile {
  display: grid;
  place-items: center;
  gap: 8px;
  color: var(--color-primary-dark);
  cursor: pointer;
}

.upload-tile:disabled {
  cursor: wait;
  opacity: 0.7;
}

.upload-tile svg {
  width: 26px;
  height: 26px;
}
</style>
