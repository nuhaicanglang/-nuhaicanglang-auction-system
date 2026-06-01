<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import SectionHeader from '@/components/common/SectionHeader.vue'
import UploadGallery from '@/components/business/UploadGallery.vue'
import { getItem, listCategories, publishItem, updateItem } from '@/api/items'
import type { Category } from '@/types/domain'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])
const images = ref<string[]>([])
const loading = ref(false)
const itemId = computed(() => route.params.id as string | undefined)
const isEdit = computed(() => Boolean(itemId.value))

const form = reactive({
  title: '',
  subtitle: '',
  description: '',
  categoryId: '',
  startPrice: '1.00',
  bidIncrement: '1.00',
  buyNowPrice: '',
  deposit: '0.00',
  duration: 1440,
  startMode: 'IMMEDIATE' as 'IMMEDIATE' | 'SCHEDULED',
  scheduledStartTime: '',
})

const rules: FormRules = {
  title: [{ required: true, min: 5, max: 100, message: '标题需为 5-100 字', trigger: 'blur' }],
  description: [{ required: true, min: 10, message: '请填写至少 10 字的拍品描述', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  startPrice: [{ required: true, message: '请输入起拍价', trigger: 'blur' }],
  bidIncrement: [{ required: true, message: '请输入加价幅度', trigger: 'blur' }],
  deposit: [{ required: true, message: '请输入保证金', trigger: 'blur' }],
}

async function loadEditData() {
  if (!itemId.value) {
    return
  }
  const item = await getItem(itemId.value)
  form.title = item.title
  form.subtitle = item.subtitle || ''
  form.description = item.description || ''
  form.categoryId = item.categoryId ? String(item.categoryId) : ''
  form.startPrice = String(item.startPrice ?? '1.00')
  form.bidIncrement = String(item.bidIncrement ?? '1.00')
  form.buyNowPrice = item.buyNowPrice ? String(item.buyNowPrice) : ''
  form.deposit = String(item.deposit ?? '0.00')
  images.value = item.images?.length ? item.images : item.coverImage ? [item.coverImage] : []
}

async function submit() {
  await formRef.value?.validate()
  if (!images.value.length) {
    ElMessage.warning('请至少上传一张拍品图片')
    return
  }
  loading.value = true
  try {
    const payload = {
      ...form,
      categoryId: form.categoryId,
      coverImage: images.value[0],
      images: images.value,
      buyNowPrice: form.buyNowPrice || undefined,
      scheduledStartTime: form.startMode === 'SCHEDULED' ? form.scheduledStartTime : undefined,
    }
    if (itemId.value) {
      await updateItem(itemId.value, payload)
      ElMessage.success('拍品已更新')
    } else {
      await publishItem(payload)
      ElMessage.success('拍品已提交审核')
    }
    router.push('/user/items')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories().catch(() => [])
  await loadEditData()
})
</script>

<template>
  <div class="page-stack">
    <SectionHeader
      :title="isEdit ? '编辑拍品' : '发布拍品'"
      description="填写拍品基础信息、价格规则和图片素材，提交后进入管理员审核流程。"
    />

    <ElCard>
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <ElFormItem label="拍品标题" prop="title"><ElInput v-model="form.title" maxlength="100" show-word-limit /></ElFormItem>
          <ElFormItem label="副标题"><ElInput v-model="form.subtitle" maxlength="200" show-word-limit /></ElFormItem>
          <ElFormItem label="分类" prop="categoryId">
            <ElSelect v-model="form.categoryId" filterable placeholder="选择分类">
              <ElOption v-for="category in categories" :key="category.id" :label="category.name" :value="String(category.id)" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="拍卖时长">
            <ElSelect v-model="form.duration">
              <ElOption label="1 小时" :value="60" />
              <ElOption label="6 小时" :value="360" />
              <ElOption label="12 小时" :value="720" />
              <ElOption label="24 小时" :value="1440" />
              <ElOption label="3 天" :value="4320" />
              <ElOption label="7 天" :value="10080" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="起拍价" prop="startPrice"><ElInput v-model="form.startPrice" inputmode="decimal" /></ElFormItem>
          <ElFormItem label="加价幅度" prop="bidIncrement"><ElInput v-model="form.bidIncrement" inputmode="decimal" /></ElFormItem>
          <ElFormItem label="一口价"><ElInput v-model="form.buyNowPrice" inputmode="decimal" placeholder="可为空" /></ElFormItem>
          <ElFormItem label="保证金" prop="deposit"><ElInput v-model="form.deposit" inputmode="decimal" /></ElFormItem>
        </div>

        <ElFormItem label="拍品图片">
          <UploadGallery v-model="images" :max="9" />
        </ElFormItem>
        <ElFormItem label="拍品描述" prop="description">
          <ElInput v-model="form.description" type="textarea" :rows="8" maxlength="10000" show-word-limit />
        </ElFormItem>
        <ElButton type="primary" :loading="loading" @click="submit">{{ isEdit ? '保存修改' : '提交审核' }}</ElButton>
      </ElForm>
    </ElCard>
  </div>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
