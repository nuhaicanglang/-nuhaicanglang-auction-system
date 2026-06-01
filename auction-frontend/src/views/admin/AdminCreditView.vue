<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminAdjustCredit, getUserCredit, listAllCreditLogs } from '@/api/admin'
import type { Credit, CreditLog } from '@/types/domain'
import { formatDateTime, normalizeRecords } from '@/utils/format'

const logs = ref<CreditLog[]>([])
const currentCredit = ref<Credit | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  userId: '',
  deltaScore: 0,
  remark: '',
})

const rules: FormRules = {
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'blur' }],
  remark: [{ required: true, message: '请输入调整原因', trigger: 'blur' }],
}

async function loadLogs() {
  logs.value = normalizeRecords(await listAllCreditLogs({ page: 1, size: 50 }).catch(() => ({ records: [] })))
}

async function queryCredit() {
  if (!form.userId) {
    return
  }
  currentCredit.value = await getUserCredit(form.userId)
}

async function adjust() {
  await formRef.value?.validate()
  await adminAdjustCredit(form.userId, { deltaScore: form.deltaScore, remark: form.remark })
  ElMessage.success('信用分已调整')
  await Promise.all([queryCredit(), loadLogs()])
}

onMounted(loadLogs)
</script>

<template>
  <div class="credit-layout">
    <ElCard>
      <template #header>信用调整</template>
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="用户 ID" prop="userId"><ElInput v-model="form.userId" @blur="queryCredit" /></ElFormItem>
        <ElFormItem label="变动分值"><ElInputNumber v-model="form.deltaScore" :min="-100" :max="100" /></ElFormItem>
        <ElFormItem label="调整原因" prop="remark"><ElInput v-model="form.remark" type="textarea" /></ElFormItem>
        <ElButton type="primary" @click="adjust">确认调整</ElButton>
      </ElForm>
      <div v-if="currentCredit" class="credit-summary">
        <strong>{{ currentCredit.score }}</strong>
        <span>{{ currentCredit.levelName || '信用等级' }}</span>
      </div>
    </ElCard>
    <ElCard>
      <template #header>信用流水</template>
      <ElTable :data="logs" border>
        <ElTableColumn prop="userId" label="用户" width="120" />
        <ElTableColumn prop="eventType" label="事件" width="150" />
        <ElTableColumn prop="deltaScore" label="变动" width="90" />
        <ElTableColumn prop="remark" label="备注" min-width="220" />
        <ElTableColumn label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
      </ElTable>
    </ElCard>
  </div>
</template>

<style scoped>
.credit-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
}

.credit-summary {
  display: grid;
  gap: 4px;
  margin-top: 18px;
  padding: 16px;
  border-radius: var(--radius-md);
  background: #f6ead7;
}

.credit-summary strong {
  font-family: var(--font-mono);
  font-size: 42px;
}

@media (max-width: 1000px) {
  .credit-layout {
    grid-template-columns: 1fr;
  }
}
</style>
