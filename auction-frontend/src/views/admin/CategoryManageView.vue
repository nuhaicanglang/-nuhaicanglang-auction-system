<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createCategory, listAdminCategories, toggleCategoryStatus, updateCategory } from '@/api/admin'
import type { Category } from '@/types/domain'

const categories = ref<Category[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<string | number | null>(null)

const form = reactive({
  parentId: '',
  name: '',
  icon: '',
  description: '',
  sortOrder: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function loadCategories() {
  categories.value = await listAdminCategories().catch(() => [])
}

function openCreate(parentId?: string | number) {
  editingId.value = null
  form.parentId = parentId ? String(parentId) : ''
  form.name = ''
  form.icon = ''
  form.description = ''
  form.sortOrder = 0
  dialogVisible.value = true
}

function openEdit(row: Category) {
  editingId.value = row.id
  form.parentId = row.parentId ? String(row.parentId) : ''
  form.name = row.name
  form.icon = row.icon || ''
  form.description = row.description || ''
  form.sortOrder = row.sortOrder ?? 0
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  const payload = { ...form, parentId: form.parentId || undefined }
  if (editingId.value) {
    await updateCategory(editingId.value, payload)
  } else {
    await createCategory(payload)
  }
  ElMessage.success('分类已保存')
  dialogVisible.value = false
  await loadCategories()
}

async function toggle(row: Category) {
  await toggleCategoryStatus(row.id)
  ElMessage.success('分类状态已切换')
  await loadCategories()
}

onMounted(loadCategories)
</script>

<template>
  <div class="page-stack">
    <div class="toolbar"><ElButton type="primary" @click="openCreate()">新增一级分类</ElButton></div>
    <ElTable :data="categories" row-key="id" border default-expand-all>
      <ElTableColumn prop="name" label="分类名称" min-width="220" />
      <ElTableColumn prop="icon" label="图标" width="120" />
      <ElTableColumn prop="description" label="描述" min-width="240" />
      <ElTableColumn prop="sortOrder" label="排序" width="100" />
      <ElTableColumn prop="status" label="状态" width="100" />
      <ElTableColumn label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" @click="openCreate(row.id)">新增子类</ElButton>
          <ElButton size="small" @click="openEdit(row)">编辑</ElButton>
          <ElButton size="small" type="warning" plain @click="toggle(row)">切换状态</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElDialog v-model="dialogVisible" title="分类信息" width="520px">
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="父级 ID"><ElInput v-model="form.parentId" placeholder="为空表示一级分类" /></ElFormItem>
        <ElFormItem label="名称" prop="name"><ElInput v-model="form.name" /></ElFormItem>
        <ElFormItem label="图标"><ElInput v-model="form.icon" /></ElFormItem>
        <ElFormItem label="描述"><ElInput v-model="form.description" type="textarea" /></ElFormItem>
        <ElFormItem label="排序"><ElInputNumber v-model="form.sortOrder" :min="0" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submit">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
}
</style>
