<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import MetricCard from '@/components/business/MetricCard.vue'
import { adminAdjustWallet, getWalletSummary, listAllWalletTransactions } from '@/api/admin'
import type { WalletSummary, WalletTransaction } from '@/types/domain'
import { formatDateTime, formatMoney, normalizeRecords } from '@/utils/format'

const summary = ref<WalletSummary>({})
const transactions = ref<WalletTransaction[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  userId: '',
  actionType: 'RECHARGE',
  amount: '',
  adminPassword: '',
  remark: '',
})

const rules: FormRules = {
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  adminPassword: [{ required: true, message: '请输入管理员密码进行二次确认', trigger: 'blur' }],
  remark: [{ required: true, message: '请输入调账原因', trigger: 'blur' }],
}

async function load() {
  const [summaryData, txPage] = await Promise.all([
    getWalletSummary().catch(() => ({})),
    listAllWalletTransactions({ page: 1, size: 30 }).catch(() => ({ records: [] })),
  ])
  summary.value = summaryData
  transactions.value = normalizeRecords(txPage)
}

async function submit() {
  await formRef.value?.validate()
  await adminAdjustWallet(form.userId, {
    actionType: form.actionType,
    amount: form.amount,
    adminPassword: form.adminPassword,
    remark: form.remark,
  })
  ElMessage.success('调账成功')
  dialogVisible.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <div class="toolbar"><ElButton type="primary" @click="dialogVisible = true">管理员调账</ElButton></div>
    <div class="grid-3">
      <MetricCard label="总余额" :value="formatMoney(summary.totalBalance)" />
      <MetricCard label="冻结总额" :value="formatMoney(summary.totalFrozenBalance)" tone="warning" />
      <MetricCard label="资金总额" :value="formatMoney(summary.totalAmount)" tone="info" />
    </div>
    <ElTable :data="transactions" border>
      <ElTableColumn prop="transactionNo" label="流水号" min-width="180" />
      <ElTableColumn prop="userId" label="用户" width="120" />
      <ElTableColumn prop="actionType" label="类型" width="130" />
      <ElTableColumn label="金额" width="130"><template #default="{ row }">{{ formatMoney(row.amount) }}</template></ElTableColumn>
      <ElTableColumn prop="remark" label="备注" min-width="220" />
      <ElTableColumn label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
    </ElTable>

    <ElDialog v-model="dialogVisible" title="管理员调账" width="520px">
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="用户 ID" prop="userId"><ElInput v-model="form.userId" /></ElFormItem>
        <ElFormItem label="操作类型">
          <ElSelect v-model="form.actionType">
            <ElOption label="充值" value="RECHARGE" />
            <ElOption label="扣款" value="DEDUCT" />
            <ElOption label="冻结" value="FREEZE" />
            <ElOption label="解冻" value="UNFREEZE" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="金额" prop="amount"><ElInput v-model="form.amount" inputmode="decimal" /></ElFormItem>
        <ElFormItem label="管理员密码" prop="adminPassword"><ElInput v-model="form.adminPassword" type="password" show-password /></ElFormItem>
        <ElFormItem label="原因" prop="remark"><ElInput v-model="form.remark" type="textarea" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submit">确认调账</ElButton>
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
