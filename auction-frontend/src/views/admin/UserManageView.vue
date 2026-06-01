<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/common/StatusTag.vue'
import { blacklistUser, changeUserStatus, listUsers, unblacklistUser } from '@/api/admin'
import type { User } from '@/types/domain'
import { formatDateTime } from '@/utils/format'

const users = ref<User[]>([])

async function loadUsers() {
  users.value = await listUsers().catch(() => [])
}

async function setStatus(user: User, status: number) {
  await changeUserStatus(user.id, status)
  ElMessage.success('用户状态已更新')
  await loadUsers()
}

async function blacklist(user: User) {
  const { value } = await ElMessageBox.prompt('请输入拉黑原因', '拉黑用户', { inputValue: '违规操作' })
  await blacklistUser(user.id, value)
  ElMessage.success('用户已拉黑')
  await loadUsers()
}

async function removeBlacklist(user: User) {
  await unblacklistUser(user.id)
  ElMessage.success('已解除拉黑')
  await loadUsers()
}

onMounted(loadUsers)
</script>

<template>
  <ElTable :data="users" border>
    <ElTableColumn prop="id" label="ID" width="110" />
    <ElTableColumn label="用户" min-width="220">
      <template #default="{ row }">
        <strong>{{ row.nickname || row.username }}</strong>
        <p class="muted">{{ row.email || row.phone }}</p>
      </template>
    </ElTableColumn>
    <ElTableColumn label="角色" min-width="180">
      <template #default="{ row }">
        <ElTag v-for="role in row.roles || []" :key="role" class="role-tag" effect="plain">{{ role }}</ElTag>
      </template>
    </ElTableColumn>
    <ElTableColumn label="状态" width="120">
      <template #default="{ row }"><StatusTag :status="row.status" :text="row.status === 2 ? '黑名单' : row.status === 0 ? '禁用' : '正常'" /></template>
    </ElTableColumn>
    <ElTableColumn label="注册时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></ElTableColumn>
    <ElTableColumn label="操作" width="300" fixed="right">
      <template #default="{ row }">
        <ElButton size="small" @click="setStatus(row, 1)">启用</ElButton>
        <ElButton size="small" type="warning" plain @click="setStatus(row, 0)">禁用</ElButton>
        <ElButton size="small" type="danger" plain @click="blacklist(row)">拉黑</ElButton>
        <ElButton size="small" @click="removeBlacklist(row)">解除</ElButton>
      </template>
    </ElTableColumn>
  </ElTable>
</template>

<style scoped>
.role-tag {
  margin: 2px 4px 2px 0;
}
</style>
